package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.service.OtpServiceApi;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.erdtman.jcs.JsonCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;


@Service
public class OtpServiceImpl implements OtpServiceApi {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    // Twilio credentials
    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    @Value("${otp.salt}")
    private String salt;

    @Value("${server.private.key}")
    private String SERVER_PRIVATE_KEY_JSON;

    @Value("${server.public.key}")
    private String SERVER_PUBLIC_KEY_JSON;


    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken); // Initialize Twilio once
    }

    @Override
    public String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int otp = 10000 + secureRandom.nextInt(90000);
        return String.valueOf(otp);
    }

    @Override
    public String sendOtp(JWK devicePub, String phoneNumber) {
        if (phoneNumber == null || !phoneNumber.matches("\\+?[1-9]\\d{1,14}")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        try {
            String otp = generateOtp();

            log.info("OTP send to phone number:{}", otp);
            // Make a JSON object out of otp, devicePub, phoneNumber and salt
            String otpJSON = "{\"otp\":\"" + otp + "\","
                    + "\"devicePub\":" + devicePub.toJSONString() + ","
                    + "\"phoneNumber\":\"" + phoneNumber + "\","
                    + "\"salt\":\"" + salt + "\"}";

            JsonCanonicalizer jc = new JsonCanonicalizer(otpJSON);
            String Input = jc.getEncodedString();
            log.info(Input);
            String otpHash = computeHash(Input);

            log.info("OTP hash:{}", otpHash);

            // Send OTP via Twilio
             Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    "Your OTP is: " + otp
            ).create();

            log.info("message:{}", message);

            return otpHash;

        } catch (Exception e) {
            throw new FailedToSendOTPException("Failed to send OTP");
        }
    }

    @Override
    public String validateOtp( String phoneNumber,JWK devicePub, String otpInput, String otpHash) {
        try {
            // Compute a new hash for the input OTP and compare it with the provided hash
            // Make a JSON object out of initiationNonce, devicePub, powNonce
            String otpJSON = "{\"otp\":\"" + otpInput + "\","
                    + "\"devicePub\":" + devicePub.toJSONString() + ","
                    + "\"phoneNumber\":\"" + phoneNumber + "\","
                    + "\"salt\":\"" + salt + "\"}";

            JsonCanonicalizer jc = new JsonCanonicalizer(otpJSON);
            String Input = jc.getEncodedString();
            log.info(Input);
            String newOtpHash = computeHash(Input);
            log.info("OTP newOtpHash:{}", newOtpHash);

            boolean isValid = newOtpHash.equals(otpHash);

            if (isValid) {
                // Generate the phone number certificate if the OTP is valid
                String certificate = generatePhoneNumberCertificate(phoneNumber, String.valueOf(devicePub));
                log.info("Certificate generated for phone number {}: {}", phoneNumber, certificate);
                return "Certificate generated: "  +  certificate;
            } else {
                log.info("OTP validation failed for phone number {}", phoneNumber);
                return "OTP validation failed";
            }
        } catch (Exception e) {
            log.error("Error validating OTP", e);
            return "Error validating the OTP";
        }
    }


    @Override
    public String computeHash(String input) {
        try {
            // Compute SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert hash to Base64
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new HashComputationException("Error computing hash");
        }
    }

    public String generatePhoneNumberCertificate(String phoneNumber, String devicePublicKey) {

        try {
            // Parse the server's private key from the JWK JSON string
            ECKey serverPrivateKey = (ECKey) JWK.parse(SERVER_PRIVATE_KEY_JSON);

            // Check that the private key contains the 'd' (private) parameter for signing
            if (serverPrivateKey.getD() == null) {
                throw new HashComputationException("Private key 'd' (private) parameter is missing.");
            }

            JWSSigner signer = new ECDSASigner(serverPrivateKey);

            // Compute hash of the phone number
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedPhoneNumber = digest.digest(phoneNumber.getBytes(StandardCharsets.UTF_8));
            String phoneHash = Base64.getEncoder().encodeToString(hashedPhoneNumber);

            // Compute hash of the device's public key
            byte[] hashedDevicePubKey = digest.digest(devicePublicKey.getBytes(StandardCharsets.UTF_8));
            String devicePubKeyHash = Base64.getEncoder().encodeToString(hashedDevicePubKey);


            // Create JWT payload including phoneHash and devicePubKeyHash
            String payloadData = String.format("{\"phoneHash\": \"%s\", \"devicePubKeyHash\": \"%s\"}", phoneHash, devicePubKeyHash);
            Payload payload = new Payload(payloadData);


            // Parse the server's public key from the JWK JSON string
            ECKey serverPublicKey = (ECKey) JWK.parse(SERVER_PUBLIC_KEY_JSON);

            // Create the JWT header with the JWK object (the server public key)
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(JOSEObjectType.JWT)
                    .jwk(serverPublicKey.toPublicJWK()) // Add JWK to the header
                    .build();

            // Build the JWS object
            JWSObject jwsObject = new JWSObject(header, payload);
            jwsObject.sign(signer);

            return jwsObject.serialize();
        } catch (Exception e) {
            // Log the exception for debugging
            log.error("Error generating device certificate", e);
            throw new HashComputationException("Error generating device certificate");
        }
    }

}
