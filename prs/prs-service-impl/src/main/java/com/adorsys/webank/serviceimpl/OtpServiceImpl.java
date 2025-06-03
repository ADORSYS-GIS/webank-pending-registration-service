package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
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
        log.debug("Generating new OTP");
        SecureRandom secureRandom = new SecureRandom();
        int otp = 10000 + secureRandom.nextInt(90000);
        return String.valueOf(otp);
    }

    @Override
    @Transactional
    public String sendOtp(JWK devicePub, String phoneNumber) {
        log.info("Processing OTP send request for phone: {}", maskPhoneNumber(phoneNumber));
        
        if (phoneNumber == null || !phoneNumber.matches("\\+?[1-9]\\d{1,14}")) {
            log.warn("Invalid phone number format received");
            throw new IllegalArgumentException("Invalid phone number format");
        }

        try {
            String otp = generateOtp();
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            
            log.debug("Generated public key hash: {}", publicKeyHash);

            // 1. First try to update existing record if found
            int updatedRows = otpRequestRepository.updateOtpByPublicKeyHash(
                    publicKeyHash,
                    otp,
                    OtpStatus.PENDING,
                    LocalDateTime.now()
            );
            
            log.debug("Updated {} existing OTP records", updatedRows);

            OtpEntity otpRequest;

            if (updatedRows == 0) {
                // 2. If no record was updated, create new one
                log.debug("No existing OTP record found, creating new one");
                otpRequest = OtpEntity.builder()
                        .phoneNumber(phoneNumber)
                        .publicKeyHash(publicKeyHash)
                        .status(OtpStatus.PENDING)
                        .build();
            } else {
                // 3. If record was updated, fetch it
                log.debug("Fetching updated OTP record");
                otpRequest = otpRequestRepository.findByPublicKeyHash(publicKeyHash)
                        .orElseThrow(() -> {
                            log.error("Failed to fetch updated OTP record for hash: {}", publicKeyHash);
                            return new FailedToSendOTPException("Failed to fetch updated OTP record");
                        });
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

            if (log.isDebugEnabled()) {
                log.debug("OTP code generated for phone: {}, OTP VALUE: {}", maskPhoneNumber(phoneNumber), otp);
            } else {
                log.info("OTP sent successfully to phone: {}", maskPhoneNumber(phoneNumber));
            }
            
            return otpHash;
        } catch (Exception e) {
            log.error("Failed to send OTP to phone: {}", maskPhoneNumber(phoneNumber), e);
            throw new FailedToSendOTPException("Failed to send OTP");
        }
    }


    @Override
    public String validateOtp(String phoneNumber, JWK devicePub, String otpInput) {
        log.info("Validating OTP for phone: {}", maskPhoneNumber(phoneNumber));
        log.debug("Validating with OTP input: {}", otpInput);
        
        try {
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            
            log.debug("Looking up OTP record for public key hash: {}", publicKeyHash);

            Optional<OtpEntity> otpEntityOpt = otpRequestRepository.findByPublicKeyHash(publicKeyHash);
            if (otpEntityOpt.isEmpty()) {
                log.warn("No OTP request found for public key hash: {}", publicKeyHash);
                return "No OTP request found for this public key";
            }

            OtpEntity otpEntity = otpEntityOpt.get();
            LocalDateTime now = LocalDateTime.now();
            if (otpEntity.getCreatedAt().isBefore(now.minusMinutes(5))) {
                log.warn("OTP expired for phone: {}. Created at: {}", 
                         maskPhoneNumber(phoneNumber), otpEntity.getCreatedAt());
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
            String newOtpHash = computeHash(input);
            
            log.debug("Comparing OTP hashes for validation");

            if (newOtpHash.equals(otpEntity.getOtpHash())) {
                log.info("OTP validated successfully for phone: {}", maskPhoneNumber(phoneNumber));
                otpEntity.setStatus(OtpStatus.COMPLETE);
                otpRequestRepository.save(otpEntity);
                return "Otp Validated Successfully";
            } else {
                log.warn("Invalid OTP provided for phone: {}", maskPhoneNumber(phoneNumber));
                otpEntity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpEntity);
                return "Invalid OTP";
            }
        } catch (Exception e) {
            log.error("Error validating OTP for phone: {}", maskPhoneNumber(phoneNumber), e);
            return "Error validating the OTP";
        }
    }

    @Override
    public String computeHash(String input) {
        try {
            log.debug("Computing hash for input");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hash = Base64.getEncoder().encodeToString(hashBytes);
            log.debug("Hash computed successfully");
            return hash;
        } catch (NoSuchAlgorithmException e) {
            log.error("Error computing hash", e);
            throw new HashComputationException("Error computing hash");
        }
    }

    private String computePublicKeyHash(String devicePublicKey) {
        log.debug("Computing hash for device public key");
        return computeHash(devicePublicKey);
    }
    
    /**
     * Masks a phone number for logging purposes
     * Shows only last 4 digits, rest are masked
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "********";
        }
        return "******" + phoneNumber.substring(Math.max(0, phoneNumber.length() - 4));
    }
}