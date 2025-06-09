package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.projection.OtpProjection;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.config.SecurityUtils;
import com.adorsys.webank.service.OtpServiceApi;
import com.nimbusds.jose.jwk.ECKey;
import org.erdtman.jcs.JsonCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

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
    public String sendOtp(String phoneNumber) {
        if (phoneNumber == null || !phoneNumber.matches("\\+?[1-9]\\d{1,14}")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        ECKey  devicePub = SecurityUtils.extractDeviceJwkFromContext();

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
                Optional<OtpProjection> otpProjectionOpt = otpRequestRepository.findByPublicKeyHash(publicKeyHash);
                if (otpProjectionOpt.isEmpty()) {
                    throw new FailedToSendOTPException("Failed to fetch updated OTP record");
                }
                OtpProjection otpProjection = otpProjectionOpt.get();
                otpRequest = new OtpEntity();
                otpRequest.setPhoneNumber(otpProjection.getPhoneNumber());
                otpRequest.setPublicKeyHash(otpProjection.getPublicKeyHash());
                otpRequest.setStatus(otpProjection.getStatus());
                otpRequest.setCreatedAt(otpProjection.getCreatedAt());
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
    public String validateOtp(String phoneNumber, String otpInput) {
        ECKey devicePub = SecurityUtils.extractDeviceJwkFromContext();

        try {
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);

            Optional<OtpProjection> otpProjectionOpt = otpRequestRepository.findByPublicKeyHash(publicKeyHash);
            if (otpProjectionOpt.isEmpty()) {
                return "No OTP request found for this public key";
            }

            OtpProjection otpProjection = otpProjectionOpt.get();
            
            // Create OTP JSON for hash verification
            String otpJSON = "{\"otp\":\"" + otpInput + "\","
                    + "\"devicePub\":" + devicePub.toJSONString() + ","
                    + "\"phoneNumber\":\"" + phoneNumber + "\","
                    + "\"salt\":\"" + salt + "\"}";

            JsonCanonicalizer jc = new JsonCanonicalizer(otpJSON);
            String input = jc.getEncodedString();
            log.info("Canonicalized input: {}", input);
            String newOtpHash = computeHash(input);
            log.info("Computed OTP hash: {}", newOtpHash);

            // Get the existing entity to update
            Optional<OtpEntity> existingOtpOpt = otpRequestRepository.findById(otpProjection.getId());
            if (existingOtpOpt.isEmpty()) {
                log.error("No OTP entity found for public key hash: {}", otpProjection.getPublicKeyHash());
                return "Error: OTP record not found";
            }

            // Update the existing entity
            OtpEntity otpEntity = existingOtpOpt.get();
            LocalDateTime now = LocalDateTime.now();
            otpEntity.setPhoneNumber(otpProjection.getPhoneNumber());
            otpEntity.setOtpCode(otpInput);
            otpEntity.setOtpHash(otpProjection.getOtpHash());
            otpEntity.setUpdatedAt(now);

            if (otpProjection.getCreatedAt().isBefore(now.minusMinutes(5))) {
                // Handle expired OTP
                otpEntity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpEntity);
                return "OTP expired. Request a new one.";
            } else if (newOtpHash.equals(otpProjection.getOtpHash())) {
                // Handle valid OTP
                otpEntity.setStatus(OtpStatus.COMPLETE);
                otpRequestRepository.save(otpEntity);
                return "Otp Validated Successfully";
            } else {
                // Handle invalid OTP
                otpEntity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpEntity);
                return "Invalid OTP";
            }
        } catch (Exception e) {
            log.error("Error validating OTP: {}", e.getMessage(), e);
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
}