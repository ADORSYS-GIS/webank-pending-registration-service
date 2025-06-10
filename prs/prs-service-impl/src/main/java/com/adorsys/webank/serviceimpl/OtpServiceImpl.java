package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.config.*;
import com.adorsys.webank.domain.*;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import com.adorsys.webank.projection.*;
import com.nimbusds.jose.jwk.*;
import jakarta.transaction.*;
import org.erdtman.jcs.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.slf4j.MDC;

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
        String correlationId = MDC.get("correlationId");
        log.debug("Generating new OTP [correlationId={}]", correlationId);
        SecureRandom secureRandom = new SecureRandom();
        int otp = 10000 + secureRandom.nextInt(90000);
        return String.valueOf(otp);
    }

    @Override
    @Transactional
    public String sendOtp(String phoneNumber) {
        String correlationId = MDC.get("correlationId");
        log.info("Processing OTP send request for phone: {} [correlationId={}]", 
                maskPhoneNumber(phoneNumber), correlationId);
        
        validatePhoneNumber(phoneNumber);
        ECKey devicePub = SecurityUtils.extractDeviceJwkFromContext();

        try {
            String otp = generateOtp();
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            
            log.debug("Generated public key hash: {} [correlationId={}]", 
                    publicKeyHash, correlationId);

            OtpEntity otpRequest = findOrCreateOtpEntity(phoneNumber, publicKeyHash, otp);
            
            // Generate and set OTP hash
            String otpHash = generateOtpHash(otp, devicePub, phoneNumber);
            otpRequest.setOtpHash(otpHash);
            otpRequest.setOtpCode(otp);
            otpRequestRepository.save(otpRequest);

            logOtpGeneration(phoneNumber, otp);
            return otpHash;
        } catch (Exception e) {
            log.error("Failed to send OTP to phone: {} [correlationId={}]", 
                    maskPhoneNumber(phoneNumber), correlationId, e);
            throw new FailedToSendOTPException("Failed to send OTP");
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        String correlationId = MDC.get("correlationId");
        if (phoneNumber == null || !phoneNumber.matches("\\+?[1-9]\\d{1,14}")) {
            log.warn("Invalid phone number format received [correlationId={}]", correlationId);
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }

    private OtpEntity findOrCreateOtpEntity(String phoneNumber, String publicKeyHash, String otp) {
        String correlationId = MDC.get("correlationId");
        
        // 1. First try to update existing record if found
        int updatedRows = otpRequestRepository.updateOtpByPublicKeyHash(
                publicKeyHash,
                otp,
                OtpStatus.PENDING,
                LocalDateTime.now()
        );
        
        log.debug("Updated {} existing OTP records [correlationId={}]", 
                updatedRows, correlationId);

        if (updatedRows == 0) {
            // 2. If no record was updated, create new one
            log.debug("No existing OTP record found, creating new one [correlationId={}]", 
                    correlationId);
            return OtpEntity.builder()
                    .phoneNumber(phoneNumber)
                    .publicKeyHash(publicKeyHash)
                    .status(OtpStatus.PENDING)
                    .build();
        } else {
            // 3. If record was updated, fetch it
            return fetchUpdatedOtpEntity(publicKeyHash);
        }
    }

    private OtpEntity fetchUpdatedOtpEntity(String publicKeyHash) {
        String correlationId = MDC.get("correlationId");
        log.debug("Fetching updated OTP record [correlationId={}]", correlationId);
        Optional<OtpProjection> otpProjectionOpt = otpRequestRepository.findByPublicKeyHash(publicKeyHash);
        if (otpProjectionOpt.isEmpty()) {
            log.error("Failed to fetch updated OTP record for hash: {} [correlationId={}]", 
                    publicKeyHash, correlationId);
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

    private String generateOtpHash(String otp, ECKey devicePub, String phoneNumber) throws NoSuchAlgorithmException {
        // Generate OTP hash
        String otpJSON = String.format(
                "{\"otp\":\"%s\",\"devicePub\":%s,\"phoneNumber\":\"%s\",\"salt\":\"%s\"}",
                otp, devicePub.toJSONString(), phoneNumber, salt
        );
        try {
            return computeHash(new JsonCanonicalizer(otpJSON).getEncodedString());
        } catch (Exception e) {
            String correlationId = MDC.get("correlationId");
            log.error("Error generating OTP hash [correlationId={}]", correlationId, e);
            throw new HashComputationException("Error generating OTP hash");
        }
    }

    private void logOtpGeneration(String phoneNumber, String otp) {
        String correlationId = MDC.get("correlationId");
        if (log.isDebugEnabled()) {
            log.debug("OTP code generated for phone: {}, OTP VALUE: {} [correlationId={}]", 
                    maskPhoneNumber(phoneNumber), otp, correlationId);
        } else {
            log.info("OTP sent successfully to phone: {} [correlationId={}]", 
                    maskPhoneNumber(phoneNumber), correlationId);
        }
    }

    @Override
    public String validateOtp(String phoneNumber, String otpInput) {
        String correlationId = MDC.get("correlationId");
        log.info("Validating OTP for phone: {} [correlationId={}]", 
                maskPhoneNumber(phoneNumber), correlationId);
        log.debug("Validating with OTP input: {} [correlationId={}]", 
                otpInput, correlationId);

        ECKey devicePub = SecurityUtils.extractDeviceJwkFromContext();

        try {
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            
            log.debug("Looking up OTP record for public key hash: {} [correlationId={}]", 
                    publicKeyHash, correlationId);

            Optional<OtpProjection> otpProjectionOpt = otpRequestRepository.findByPublicKeyHash(publicKeyHash);
            if (otpProjectionOpt.isEmpty()) {
                log.warn("No OTP request found for public key hash: {} [correlationId={}]", 
                        publicKeyHash, correlationId);
                return "No OTP request found for this public key";
            }

            OtpProjection otpProjection = otpProjectionOpt.get();
            
            // Get the existing entity to update
            Optional<OtpEntity> existingOtpOpt = otpRequestRepository.findById(otpProjection.getId());
            if (existingOtpOpt.isEmpty()) {
                log.error("No OTP entity found for public key hash: {} [correlationId={}]", 
                        otpProjection.getPublicKeyHash(), correlationId);
                return "Error: OTP record not found";
            }

            // Create OTP JSON for hash verification
            String otpJSON = "{\"otp\":\"" + otpInput + "\","
                    + "\"devicePub\":" + devicePub.toJSONString() + ","
                    + "\"phoneNumber\":\"" + phoneNumber + "\","
                    + "\"salt\":\"" + salt + "\"}";

            JsonCanonicalizer jc = new JsonCanonicalizer(otpJSON);
            String input = jc.getEncodedString();
            log.debug("Canonicalized input for validation [correlationId={}]", correlationId);
            String newOtpHash = computeHash(input);
            
            // Update the existing entity and handle OTP verification
            OtpEntity otpEntity = existingOtpOpt.get();
            updateOtpEntity(otpEntity, otpProjection, otpInput);
            
            LocalDateTime now = LocalDateTime.now();
            if (otpProjection.getCreatedAt().isBefore(now.minusMinutes(5))) {
                return handleExpiredOtp(phoneNumber, otpEntity, otpProjection);
            } else if (newOtpHash.equals(otpProjection.getOtpHash())) {
                return handleValidOtp(phoneNumber, otpEntity);
            } else {
                return handleInvalidOtp(phoneNumber, otpEntity);
            }
        } catch (Exception e) {
            log.error("Error validating OTP for phone: {} [correlationId={}]", 
                    maskPhoneNumber(phoneNumber), correlationId, e);
            return "Error validating the OTP";
        }
    }
    
    private void updateOtpEntity(OtpEntity otpEntity, OtpProjection otpProjection, String otpInput) {
        LocalDateTime now = LocalDateTime.now();
        otpEntity.setPhoneNumber(otpProjection.getPhoneNumber());
        otpEntity.setOtpCode(otpInput);
        otpEntity.setOtpHash(otpProjection.getOtpHash());
        otpEntity.setUpdatedAt(now);
    }
    
    private String handleExpiredOtp(String phoneNumber, OtpEntity otpEntity, OtpProjection otpProjection) {
        String correlationId = MDC.get("correlationId");
        log.warn("OTP expired for phone: {}. Created at: {} [correlationId={}]", 
                maskPhoneNumber(phoneNumber), otpProjection.getCreatedAt(), correlationId);
        otpEntity.setStatus(OtpStatus.INCOMPLETE);
        otpRequestRepository.save(otpEntity);
        return "OTP expired. Request a new one.";
    }
    
    private String handleValidOtp(String phoneNumber, OtpEntity otpEntity) {
        String correlationId = MDC.get("correlationId");
        log.info("OTP validated successfully for phone: {} [correlationId={}]", 
                maskPhoneNumber(phoneNumber), correlationId);
        otpEntity.setStatus(OtpStatus.COMPLETE);
        otpRequestRepository.save(otpEntity);
        return "Otp Validated Successfully";
    }
    
    private String handleInvalidOtp(String phoneNumber, OtpEntity otpEntity) {
        String correlationId = MDC.get("correlationId");
        log.warn("Invalid OTP provided for phone: {} [correlationId={}]", 
                maskPhoneNumber(phoneNumber), correlationId);
        otpEntity.setStatus(OtpStatus.INCOMPLETE);
        otpRequestRepository.save(otpEntity);
        return "Invalid OTP";
    }

    @Override
    public String computeHash(String input) {
        String correlationId = MDC.get("correlationId");
        try {
            log.debug("Computing hash for input [correlationId={}]", correlationId);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hash = Base64.getEncoder().encodeToString(hashBytes);
            log.debug("Hash computed successfully [correlationId={}]", correlationId);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            log.error("Error computing hash [correlationId={}]", correlationId, e);
            throw new HashComputationException("Error computing hash");
        }
    }

    private String computePublicKeyHash(String devicePublicKey) {
        String correlationId = MDC.get("correlationId");
        log.debug("Computing hash for device public key [correlationId={}]", correlationId);
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