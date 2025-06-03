package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.security.HashHelper;
import com.adorsys.webank.service.OtpServiceApi;
import com.adorsys.webank.projection.OtpProjection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;


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
    
    @Value("${otp.salt}")
    private String salt;

    @Override
    public String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int otp = 10000 + secureRandom.nextInt(90000);
        return String.valueOf(otp);
    }

    @Override
    @Transactional
    public String sendOtp(JWK devicePub, String phoneNumber) {
        validatePhoneNumber(phoneNumber);
        String otp = generateOtp();
        String publicKeyHash = calculatePublicKeyHash(devicePub);
        
        // 1. Try to update existing record
        int updatedRows = updateExistingOtpRecord(publicKeyHash, otp);
        
        try {
            // 2. Generate OTP hash
            String canonicalJson = generateCanonicalJson(otp, devicePub, phoneNumber);
            
            // 3. Create or retrieve OTP entity
            OtpEntity otpRequest = createOrRetrieveOtpEntity(updatedRows, publicKeyHash, phoneNumber);
            
            // 4. Save OTP record with hash
            return saveOtpRecord(otpRequest, otp, canonicalJson, phoneNumber);
        } catch (Exception e) {
            return handleOtpSendException(e);
        }
    }
    
    /**
     * Validates phone number format
     */
    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || !phoneNumber.matches("\\+?[1-9]\\d{1,14}")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }
    
    /**
     * Calculate public key hash for lookup
     */
    private String calculatePublicKeyHash(JWK devicePub) {
        String devicePublicKey = devicePub.toJSONString();
        String publicKeyHash = hashHelper.calculateSHA256AsHex(devicePublicKey);
        if (log.isDebugEnabled()) {
            log.debug("Generated public key hash for storage: {}", publicKeyHash);
        }
        return publicKeyHash;
    }
    
    /**
     * Update existing OTP record if found
     */
    private int updateExistingOtpRecord(String publicKeyHash, String otp) {
        return otpRequestRepository.updateOtpByPublicKeyHash(
                publicKeyHash,
                otp,
                OtpStatus.PENDING,
                LocalDateTime.now()
        );
    }
    
    /**
     * Generate canonical JSON for hashing
     */
    private String generateCanonicalJson(String otp, JWK devicePub, String phoneNumber) throws IOException {
        // Create direct JSON string format for canonicalization
        return new JsonCanonicalizer(String.format(
            "{\"otp\":\"%s\",\"devicePub\":%s,\"phoneNumber\":\"%s\",\"salt\":\"%s\"}",
            otp, devicePub.toJSONString(), phoneNumber, salt
        )).getEncodedString();
    }
    
    /**
     * Create new or retrieve existing OTP entity based on update status
     */
    private OtpEntity createOrRetrieveOtpEntity(int updatedRows, String publicKeyHash, String phoneNumber) {
        if (updatedRows == 0) {
            // If no record was updated, create new one
            return OtpEntity.builder()
                    .phoneNumber(phoneNumber)
                    .publicKeyHash(publicKeyHash)
                    .status(OtpStatus.PENDING)
                    .build();
        } else {
            // If record was updated, fetch it
            return retrieveUpdatedOtpEntity(publicKeyHash);
        }
    }
    
    /**
     * Retrieve the updated OTP entity from repository
     */
    private OtpEntity retrieveUpdatedOtpEntity(String publicKeyHash) {
        Optional<OtpProjection> otpProjectionOpt = otpRequestRepository.findByPublicKeyHash(publicKeyHash);
        if (otpProjectionOpt.isEmpty()) {
            throw new FailedToSendOTPException("Failed to fetch updated OTP record");
        }
        
        OtpProjection otpProjection = otpProjectionOpt.get();
        OtpEntity otpRequest = new OtpEntity();
        otpRequest.setPhoneNumber(otpProjection.getPhoneNumber());
        otpRequest.setPublicKeyHash(otpProjection.getPublicKeyHash());
        otpRequest.setStatus(otpProjection.getStatus());
        otpRequest.setCreatedAt(otpProjection.getCreatedAt());
        return otpRequest;
    }
    
    /**
     * Save OTP record with hash and return the hash
     */
    private String saveOtpRecord(OtpEntity otpRequest, String otp, String canonicalJson, String phoneNumber) {
        // Encode the hash only when needed, right before saving
        String otpHash = passwordEncoder.encode(canonicalJson);
        
        otpRequest.setOtpHash(otpHash);
        otpRequest.setOtpCode(otp);
        otpRequestRepository.save(otpRequest);
        
        if (log.isInfoEnabled()) {
            log.info("OTP sent successfully to phone number: {}, with otp: {}", phoneNumber, otp);
        }
        return otpHash;
    }
    
    /**
     * Handle exceptions during OTP sending process
     */
    private String handleOtpSendException(Exception e) {
        if (log.isErrorEnabled()) {
            if (e instanceof JsonProcessingException) {
                log.error("Failed to serialize OTP data to JSON", e);
            } else if (e instanceof IOException) {
                log.error("Failed to canonicalize JSON for OTP data", e);
            }
        }
        throw new FailedToSendOTPException("Failed to process OTP data", e);
    }

    @Override
    public String validateOtp(String phoneNumber, JWK devicePub, String otpInput) {
        // 1. Find the OTP request by public key hash
        String publicKeyHash = calculatePublicKeyHash(devicePub);
        // Check debug log level before logging
        if (log.isDebugEnabled()) {
            log.debug("Finding OTP request by public key hash: {}", publicKeyHash);
        }
        OtpEntity otpEntity = findOtpRequestByHash(publicKeyHash);
        
        // 2. Check if OTP is expired
        checkOtpExpiration(otpEntity);
        
        // 3. Create OTP data POJO for validation
        OtpData otpData = createOtpDataForValidation(phoneNumber, devicePub, otpInput);
        
        // 4. Verify OTP hash
        return verifyOtpHash(otpData, otpEntity);
    }
    

    
    /**
     * Find OTP request by public key hash
     */
    private OtpEntity findOtpRequestByHash(String publicKeyHash) {
        return otpRequestRepository.findEntityByPublicKeyHash(publicKeyHash)
                .orElseThrow(() -> new OtpValidationException("No OTP request found for this public key"));
    }
    
    /**
     * Check if OTP is expired (5 minutes validity)
     */
    private void checkOtpExpiration(OtpEntity otpEntity) {
        if (otpEntity.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            if (log.isWarnEnabled()) {
                log.warn("OTP expired for id: {}", otpEntity.getId());
            }
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
                .salt(salt)
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
                return "Otp Validated Successfully";
            } else {
                otpEntity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpEntity);
                throw new OtpValidationException("Invalid OTP");
            }
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to serialize OTP validation data to JSON", e);
            }
            otpEntity.setStatus(OtpStatus.INCOMPLETE);
            otpRequestRepository.save(otpEntity);
            throw new OtpValidationException("Error processing OTP data", e);
        }
    }

    /**
     * Compute hash from input string using SHA-256
     */
    public String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new HashComputationException("Error computing hash");
        }
    }
}