package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import com.nimbusds.jose.jwk.*;
import jakarta.transaction.*;
import org.erdtman.jcs.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.util.*;

import com.adorsys.error.ValidationException;

@Service
public class OtpServiceImpl implements OtpServiceApi {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    // Field declarations moved to the top
    private final OtpRequestRepository otpRequestRepository;

    @Value("${otp.salt}")
    private String salt;

    // Constructor
    public OtpServiceImpl(OtpRequestRepository otpRequestRepository) {
        this.otpRequestRepository = otpRequestRepository;
    }


    @Override
    public String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int otp = 10000 + secureRandom.nextInt(90000);
        return String.valueOf(otp);
    }

    @Override
    @Transactional
    public String sendOtp(JWK devicePub, String phoneNumber) {
        if (phoneNumber == null || !phoneNumber.matches("\\+?[1-9]\\d{1,14}")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        try {
            String otp = generateOtp();
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);

            // 1. First try to update existing record if found
            int updatedRows = otpRequestRepository.updateOtpByPublicKeyHash(
                    publicKeyHash,
                    otp,
                    OtpStatus.PENDING,
                    LocalDateTime.now()
            );

            OtpEntity otpRequest;

            if (updatedRows == 0) {
                // 2. If no record was updated, create new one
                otpRequest = OtpEntity.builder()
                        .phoneNumber(phoneNumber)
                        .publicKeyHash(publicKeyHash)
                        .status(OtpStatus.PENDING)
                        .build();
            } else {
                // 3. If record was updated, fetch it
                otpRequest = otpRequestRepository.findByPublicKeyHash(publicKeyHash)
                        .orElseThrow(() -> new FailedToSendOTPException("Failed to fetch updated OTP record"));
            }

            // Generate OTP hash
            String otpJSON = String.format(
                    "{\"otp\":\"%s\",\"devicePub\":%s,\"phoneNumber\":\"%s\",\"salt\":\"%s\"}",
                    otp, devicePub.toJSONString(), phoneNumber, salt
            );
            String otpHash = computeHash(new JsonCanonicalizer(otpJSON).getEncodedString());

            // Set hash and save
            otpRequest.setOtpHash(otpHash);
            otpRequest.setOtpCode(otp);
            otpRequestRepository.save(otpRequest);

            log.info("OTP code for {}  is  {}",   phoneNumber, otpRequest.getOtpCode());
            return otpHash;
        } catch (Exception e) {
            log.error("Failed to send OTP to {}", phoneNumber, e);
            throw new FailedToSendOTPException("Failed to send OTP");
        }
    }


    @Override
    public String validateOtp(String phoneNumber, JWK devicePub, String otpInput) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new ValidationException("Phone number is required");
        }
        if (otpInput == null || otpInput.isEmpty()) {
            throw new ValidationException("OTP is required");
        }
        try {
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);

            Optional<OtpEntity> otpEntityOpt = otpRequestRepository.findByPublicKeyHash(publicKeyHash);
            if (otpEntityOpt.isEmpty()) {
                return "No OTP request found for this public key";
            }

            OtpEntity otpEntity = otpEntityOpt.get();
            LocalDateTime now = LocalDateTime.now();
            if (otpEntity.getCreatedAt().isBefore(now.minusMinutes(5))) {
                otpEntity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpEntity);
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

            if (newOtpHash.equals(otpEntity.getOtpHash())) {
                otpEntity.setStatus(OtpStatus.COMPLETE);
                otpRequestRepository.save(otpEntity);
                return "Otp Validated Successfully";
            } else {
                otpEntity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpEntity);
                throw new ValidationException("Invalid OTP");
            }
        } catch (Exception e) {
            log.error("Error validating OTP", e);
            throw new ValidationException("Error validating the OTP");
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

}