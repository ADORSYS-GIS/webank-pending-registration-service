package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.security.HashHelper;
import com.adorsys.webank.service.OtpServiceApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;


import com.adorsys.webank.exceptions.OtpValidationException;
import com.adorsys.webank.model.OtpData;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpServiceApi {
    private final OtpRequestRepository otpRequestRepository;
    private final HashHelper hashHelper;
    private final ObjectMapper objectMapper;
    
    // Using default Spring Security recommended parameters
    private final Argon2PasswordEncoder passwordEncoder = new Argon2PasswordEncoder(16, 32, 1, 4096, 2);

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
        String otp = generateOtp();
        String devicePublicKey = devicePub.toJSONString();
        // Use deterministic hash for lookup key
        String publicKeyHash = hashHelper.calculateSHA256AsHex(devicePublicKey);
        log.debug("Generated public key hash for storage: {}", publicKeyHash);

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

        // Generate OTP hash using structured POJO instead of Map
        OtpData otpData = OtpData.builder()
                .otp(otp)
                .devicePub(devicePub)
                .phoneNumber(phoneNumber)
                .build();
        
        try {
            String otpJSON = objectMapper.writeValueAsString(otpData);
            String canonicalJson = new JsonCanonicalizer(otpJSON).getEncodedString();
            String otpHash = passwordEncoder.encode(canonicalJson);

            // Set hash and save
            otpRequest.setOtpHash(otpHash);
            otpRequest.setOtpCode(otp);
            otpRequestRepository.save(otpRequest);

            log.info("OTP sent successfully to phone number: {}, with otp: {}", phoneNumber, otp);
            return otpHash;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OTP data to JSON", e);
            throw new FailedToSendOTPException("Failed to process OTP data", e);
        } catch (IOException e) {
            log.error("Failed to canonicalize JSON for OTP data", e);
            throw new FailedToSendOTPException("Failed to process OTP data", e);
        }
    }

    @Override
    public String validateOtp(String phoneNumber, JWK devicePub, String otpInput) {
        // 1. Find the OTP request by public key hash
        String publicKeyHash = calculatePublicKeyHash(devicePub);
        OtpEntity otpEntity = findOtpRequestByHash(publicKeyHash);
        
        // 2. Check if OTP is expired
        checkOtpExpiration(otpEntity);
        
        // 3. Create OTP data POJO for validation
        OtpData otpData = createOtpDataForValidation(phoneNumber, devicePub, otpInput);
        
        // 4. Verify OTP hash
        return verifyOtpHash(otpData, otpEntity);
    }
    
    /**
     * Calculate hash from device public key
     */
    private String calculatePublicKeyHash(JWK devicePub) {
        String devicePublicKey = devicePub.toJSONString();
        String publicKeyHash = hashHelper.calculateSHA256AsHex(devicePublicKey);
        log.debug("Generated public key hash for lookup: {}", publicKeyHash);
        return publicKeyHash;
    }
    
    /**
     * Find OTP request by public key hash
     */
    private OtpEntity findOtpRequestByHash(String publicKeyHash) {
        return otpRequestRepository.findByPublicKeyHash(publicKeyHash)
                .orElseThrow(() -> new OtpValidationException("No OTP request found for this public key"));
    }
    
    /**
     * Check if OTP is expired (5 minutes validity)
     */
    private void checkOtpExpiration(OtpEntity otpEntity) {
        if (otpEntity.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            log.warn("OTP expired for id: {}", otpEntity.getId());
            otpEntity.setStatus(OtpStatus.INCOMPLETE); // Using INCOMPLETE as there's no EXPIRED status
            otpRequestRepository.save(otpEntity);
            throw new OtpValidationException("OTP expired. Request a new one.");
        }
    }
    
    /**
     * Create OTP data object for validation
     */
    private OtpData createOtpDataForValidation(String phoneNumber, JWK devicePub, String otpInput) {
        return OtpData.builder()
                .otp(otpInput)
                .devicePub(devicePub)
                .phoneNumber(phoneNumber)
                .build();
    }
    
    /**
     * Verify OTP hash and update entity status
     */
    private String verifyOtpHash(OtpData otpData, OtpEntity otpEntity) {
        try {
            String otpJSON = objectMapper.writeValueAsString(otpData);
            String canonicalJson = new JsonCanonicalizer(otpJSON).getEncodedString();
            
            if (log.isDebugEnabled()) {
                log.debug("OTP validation input: {}", canonicalJson);
            }

            if (passwordEncoder.matches(canonicalJson, otpEntity.getOtpHash())) {
                otpEntity.setStatus(OtpStatus.COMPLETE);
                otpRequestRepository.save(otpEntity);
                return "OTP Validated Successfully";
            } else {
                otpEntity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpEntity);
                throw new OtpValidationException("Invalid OTP");
            }
        } catch (IOException e) {
            log.error("Failed to serialize OTP validation data to JSON", e);
            otpEntity.setStatus(OtpStatus.INCOMPLETE);
            otpRequestRepository.save(otpEntity);
            throw new OtpValidationException("Error processing OTP data", e);
        }
    }
}