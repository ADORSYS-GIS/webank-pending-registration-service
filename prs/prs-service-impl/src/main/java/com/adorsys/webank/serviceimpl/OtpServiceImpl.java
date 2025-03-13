package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.service.OtpServiceApi;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.erdtman.jcs.JsonCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.adorsys.webank.repository.OtpRequestRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import com.adorsys.webank.domain.OtpRequest;
import com.adorsys.webank.domain.OtpStatus;
import  java.util.Optional;
import java.time.LocalDateTime;


@Service
public class OtpServiceImpl implements OtpServiceApi {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    private OtpRequestRepository otpRequestRepository;
    public OtpServiceImpl(OtpRequestRepository otpRequestRepository) {
        this.otpRequestRepository = otpRequestRepository;
    }
    // Twilio credentials
    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    @Value("${otp.salt}")
    String salt;

    @Value("${server.private.key}")
    private String SERVER_PRIVATE_KEY_JSON;

    @Value("${server.public.key}")
    private String SERVER_PUBLIC_KEY_JSON;

    @Autowired

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
            log.info("OTP sent to phone number:{}", otp);

            // Extract raw public key string
            String devicePublicKey = devicePub.toJSONString();

            // Compute public key hash
            String publicKeyHash = computePublicKeyHash(devicePublicKey);


            // Make a JSON object out of otp, devicePub, phoneNumber, and salt
            String otpJSON = "{\"otp\":\"" + otp + "\","
                    + "\"devicePub\":" + devicePub.toJSONString() + ","
                    + "\"phoneNumber\":\"" + phoneNumber + "\","
                    + "\"salt\":\"" + salt + "\"}";

            JsonCanonicalizer jc = new JsonCanonicalizer(otpJSON);
            String input = jc.getEncodedString();
            log.info(input);
            String otpHash = computeHash(input);
            log.info("OTP hash:{}", otpHash);

            // save to database
            OtpRequest otpRequest = OtpRequest.builder()
                    .phoneNumber(phoneNumber)
                    .publicKeyHash(publicKeyHash)
                    .otpHash(otpHash)
                    .otpcode(otp)
                    .status(OtpStatus.PENDING)
                    .build();
            otpRequestRepository.save(otpRequest);

            log.info("Message sent to phone number: {}", otp);

            return otpHash;

        } catch (Exception e) {
            throw new FailedToSendOTPException("Failed to send OTP");
        }
    }

    @Override
    public String validateOtp(String phoneNumber, JWK devicePub, String otpInput, String otpHash) {
        try {


            // Extract raw public key string (matches sendOtp logic)
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);


            // Retrieve stored OTP request
            Optional<OtpRequest> otpRequestOpt = otpRequestRepository.findByPublicKeyHash(publicKeyHash);
            if (otpRequestOpt.isEmpty()) {
                return "No OTP request found for this public key";
            }

            OtpRequest otpRequest = otpRequestOpt.get();

            // Check expiration (5 minutes)
            LocalDateTime now = LocalDateTime.now();
            if (otpRequest.getCreatedAt().isBefore(now.minusMinutes(5))) {
                otpRequest.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpRequest);
                return "OTP expired. Request a new one.";
            }


            // Compute a new hash for the input OTP and compare it with the provided hash
            String otpJSON = "{\"otp\":\"" + otpInput + "\","
                    + "\"devicePub\":" + devicePub.toJSONString() + ","
                    + "\"phoneNumber\":\"" + phoneNumber + "\","
                    + "\"salt\":\"" + salt + "\"}";

            JsonCanonicalizer jc = new JsonCanonicalizer(otpJSON);
            String input = jc.getEncodedString();
            log.info(input);
            String newOtpHash = computeHash(input);
            log.info("OTP newOtpHash:{}", newOtpHash);


            // Compare with stored hash
            if (newOtpHash.equals(otpRequest.getOtpHash())) {
                otpRequest.setStatus(OtpStatus.COMPLETE);
                otpRequestRepository.save(otpRequest);
                return "Certificate generated: " + generatePhoneNumberCertificate(phoneNumber, devicePublicKey);
            } else {
                otpRequest.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpRequest);
                return "Invalid OTP";
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


    private String computePublicKeyHash(String devicePublicKey) throws Exception {
        return computeHash(devicePublicKey);
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

            // Create the JWT header with the JWK object (the server public key)
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(JOSEObjectType.JWT)
                    .build();

            // Build the JWS object
            JWSObject jwsObject = new JWSObject(header, payload);
            jwsObject.sign(signer);

            return jwsObject.serialize();
        } catch (Exception e) {
            log.error("Error generating device certificate", e);
            throw new HashComputationException("Error generating device certificate");
        }
    }
}