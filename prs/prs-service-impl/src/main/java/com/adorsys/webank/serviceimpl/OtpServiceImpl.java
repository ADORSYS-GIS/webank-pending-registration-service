package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.config.*;
import com.adorsys.webank.domain.*;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import com.adorsys.webank.projection.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import jakarta.transaction.*;
import org.erdtman.jcs.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.slf4j.MDC;

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
        
        if (phoneNumber == null || !phoneNumber.matches("\\+?[1-9]\\d{1,14}")) {
            log.warn("Invalid phone number format received [correlationId={}]", correlationId);
            throw new IllegalArgumentException("Invalid phone number format");
        }

        ECKey devicePub = SecurityUtils.extractDeviceJwkFromContext();

        try {
            String otp = generateOtp();
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            
            log.debug("Generated public key hash: {} [correlationId={}]", 
                    publicKeyHash, correlationId);

            // 1. First try to update existing record if found
            int updatedRows = otpRequestRepository.updateOtpByPublicKeyHash(
                    publicKeyHash,
                    otp,
                    OtpStatus.PENDING,
                    LocalDateTime.now()
            );
            
            log.debug("Updated {} existing OTP records [correlationId={}]", 
                    updatedRows, correlationId);

            OtpEntity otpRequest;

            if (updatedRows == 0) {
                // 2. If no record was updated, create new one
                log.debug("No existing OTP record found, creating new one [correlationId={}]", 
                        correlationId);
                otpRequest = OtpEntity.builder()
                        .phoneNumber(phoneNumber)
                        .publicKeyHash(publicKeyHash)
                        .status(OtpStatus.PENDING)
                        .build();
            } else {
                // 3. If record was updated, fetch it
                log.debug("Fetching updated OTP record [correlationId={}]", correlationId);
                Optional<OtpProjection> otpProjectionOpt = otpRequestRepository.findByPublicKeyHash(publicKeyHash);
                if (otpProjectionOpt.isEmpty()) {
                    log.error("Failed to fetch updated OTP record for hash: {} [correlationId={}]", 
                            publicKeyHash, correlationId);
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

            if (log.isDebugEnabled()) {
                log.debug("OTP code generated for phone: {}, OTP VALUE: {} [correlationId={}]", 
                        maskPhoneNumber(phoneNumber), otp, correlationId);
            } else {
                log.info("OTP sent successfully to phone: {} [correlationId={}]", 
                        maskPhoneNumber(phoneNumber), correlationId);
            }
            
            return otpHash;
        } catch (Exception e) {
            log.error("Failed to send OTP to phone: {} [correlationId={}]", 
                    maskPhoneNumber(phoneNumber), correlationId, e);
            throw new FailedToSendOTPException("Failed to send OTP");
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
            LocalDateTime now = LocalDateTime.now();
            if (otpProjection.getCreatedAt().isBefore(now.minusMinutes(5))) {
                log.warn("OTP expired for phone: {}. Created at: {} [correlationId={}]", 
                        maskPhoneNumber(phoneNumber), otpProjection.getCreatedAt(), correlationId);
                OtpEntity otpEntity = new OtpEntity();
                otpEntity.setPhoneNumber(otpProjection.getPhoneNumber());
                otpEntity.setPublicKeyHash(otpProjection.getPublicKeyHash());
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
            
            log.debug("Comparing OTP hashes for validation [correlationId={}]", correlationId);

            if (newOtpHash.equals(otpProjection.getOtpHash())) {
                log.info("OTP validated successfully for phone: {} [correlationId={}]", 
                        maskPhoneNumber(phoneNumber), correlationId);
                OtpEntity otpEntity = new OtpEntity();
                otpEntity.setPhoneNumber(otpProjection.getPhoneNumber());
                otpEntity.setPublicKeyHash(otpProjection.getPublicKeyHash());
                otpEntity.setStatus(OtpStatus.COMPLETE);
                otpRequestRepository.save(otpEntity);
                return "Otp Validated Successfully";
            } else {
                log.warn("Invalid OTP provided for phone: {} [correlationId={}]", 
                        maskPhoneNumber(phoneNumber), correlationId);
                OtpEntity otpEntity = new OtpEntity();
                otpEntity.setPhoneNumber(otpProjection.getPhoneNumber());
                otpEntity.setPublicKeyHash(otpProjection.getPublicKeyHash());
                otpEntity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpEntity);
                return "Invalid OTP";
            }
        } catch (Exception e) {
            log.error("Error validating OTP for phone: {} [correlationId={}]", 
                    maskPhoneNumber(phoneNumber), correlationId, e);
            return "Error validating the OTP";
        }
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