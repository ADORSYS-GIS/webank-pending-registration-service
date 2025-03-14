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
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class OtpServiceImpl implements OtpServiceApi {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    // Field declarations moved to the top
    private final OtpRequestRepository otpRequestRepository;

    // Twilio credentials and other @Value fields
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

    // Constructor
    public OtpServiceImpl(OtpRequestRepository otpRequestRepository) {
        this.otpRequestRepository = otpRequestRepository;
    }

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken); // Initialize Twilio once
    }

    // Remaining methods...

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

            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);

            String otpJSON = "{\"otp\":\"" + otp + "\","
                    + "\"devicePub\":" + devicePub.toJSONString() + ","
                    + "\"phoneNumber\":\"" + phoneNumber + "\","
                    + "\"salt\":\"" + salt + "\"}";

            JsonCanonicalizer jc = new JsonCanonicalizer(otpJSON);
            String input = jc.getEncodedString();
            log.info(input);
            String otpHash = computeHash(input);
            log.info("OTP hash:{}", otpHash);

            OtpRequest otpRequest = OtpRequest.builder()
                    .phoneNumber(phoneNumber)
                    .publicKeyHash(publicKeyHash)
                    .otpHash(otpHash)
                    .otpCode(otp)
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
    public String validateOtp(String phoneNumber, JWK devicePub, String otpInput) {
        try {
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);

            Optional<OtpRequest> otpRequestOpt = otpRequestRepository.findByPublicKeyHash(publicKeyHash);
            if (otpRequestOpt.isEmpty()) {
                return "No OTP request found for this public key";
            }

            OtpRequest otpRequest = otpRequestOpt.get();
            LocalDateTime now = LocalDateTime.now();
            if (otpRequest.getCreatedAt().isBefore(now.minusMinutes(5))) {
                otpRequest.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpRequest);
                return "OTP expired. Request a new one.";
            }

            String otpJSON = "{\"otp\":\"" + otpInput + "\","
                    + "\"devicePub\":" + devicePub.toJSONString() + ","
                    + "\"phoneNumber\":\"" + phoneNumber + "\","
                    + "\"salt\":\"" + salt + "\"}";

            JsonCanonicalizer jc = new JsonCanonicalizer(otpJSON);
            String input = jc.getEncodedString();
            log.info(input);
            String newOtpHash = computeHash(input);
            log.info("OTP newOtpHash:{}", newOtpHash);

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
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new HashComputationException("Error computing hash");
        }
    }

    private String computePublicKeyHash(String devicePublicKey) {
        return computeHash(devicePublicKey);
    }

    public String generatePhoneNumberCertificate(String phoneNumber, String devicePublicKey) {
        try {
            ECKey serverPrivateKey = (ECKey) JWK.parse(SERVER_PRIVATE_KEY_JSON);
            if (serverPrivateKey.getD() == null) {
                throw new HashComputationException("Private key 'd' (private) parameter is missing.");
            }

            JWSSigner signer = new ECDSASigner(serverPrivateKey);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedPhoneNumber = digest.digest(phoneNumber.getBytes(StandardCharsets.UTF_8));
            String phoneHash = Base64.getEncoder().encodeToString(hashedPhoneNumber);

            byte[] hashedDevicePubKey = digest.digest(devicePublicKey.getBytes(StandardCharsets.UTF_8));
            String devicePubKeyHash = Base64.getEncoder().encodeToString(hashedDevicePubKey);

            String payloadData = String.format("{\"phoneHash\": \"%s\", \"devicePubKeyHash\": \"%s\"}", phoneHash, devicePubKeyHash);
            Payload payload = new Payload(payloadData);

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(JOSEObjectType.JWT)
                    .build();

            JWSObject jwsObject = new JWSObject(header, payload);
            jwsObject.sign(signer);

            return jwsObject.serialize();
        } catch (Exception e) {
            log.error("Error generating device certificate", e);
            throw new HashComputationException("Error generating device certificate");
        }
    }
}