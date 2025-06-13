package com.adorsys.webank.serviceimpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.erdtman.jcs.JsonCanonicalizer;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.adorsys.error.ValidationException;
import com.adorsys.webank.config.SecurityUtils;
import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.dto.response.OtpResponse;
import com.adorsys.webank.dto.response.OtpValidationResponse;
import com.adorsys.webank.model.OtpData;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.service.OtpServiceApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpServiceApi {

    private final OtpRequestRepository otpRequestRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

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
    public OtpResponse sendOtp(String phoneNumber) {
        String correlationId = MDC.get("correlationId");
        log.info("Processing OTP send request for phone: {} [correlationId={}]", 
                maskPhoneNumber(phoneNumber), correlationId);
        
        validatePhoneNumber(phoneNumber);
        ECKey devicePub = SecurityUtils.extractDeviceJwkFromContext();

        String otp = generateOtp();
        String devicePublicKey = devicePub.toJSONString();

        // Use deterministic hash for lookup key
        String publicKeyHash = computeHash(devicePublicKey);
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
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            // 3. If record was updated, fetch it
            otpRequest = otpRequestRepository.findEntityByPublicKeyHash(publicKeyHash)
                    .orElseThrow(() -> new ValidationException("Failed to fetch updated OTP record"));
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

            log.info("OTP sent successfully to phone: {} [correlationId={}]", phoneNumber, correlationId);

            // Build response DTO
            OtpResponse response = OtpResponse.builder()
                    .otpHash(publicKeyHash)
                    .phoneNumber(phoneNumber)
                    .expiresAt(otpRequest.getCreatedAt().plusSeconds(300))
                    .validitySeconds(300)
                    .sent(true)
                    .build();
            response.setOtpHash(publicKeyHash);
            response.setPhoneNumber(phoneNumber);
            response.setExpiresAt(otpRequest.getCreatedAt().plusSeconds(300));
            response.setValiditySeconds(300);
            response.setSent(true);
            return response;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OTP hash data", e);
            throw new ValidationException("Failed to compute OTP hash: " + e.getMessage());
        } catch (IOException e) {
            log.error("I/O error while processing OTP request", e);
            throw new ValidationException("Failed to process OTP request due to I/O error");
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        String correlationId = MDC.get("correlationId");
        if (phoneNumber == null || !phoneNumber.matches("\\+?[1-9]\\d{1,14}")) {
            log.warn("Invalid phone number format received [correlationId={}]", correlationId);
            throw new ValidationException("Invalid phone number format");
        }
    }

    @Override
    public OtpValidationResponse validateOtp(String phoneNumber, String otpInput) {
        String correlationId = MDC.get("correlationId");
        log.info("Validating OTP for phone: {} [correlationId={}]", 
                maskPhoneNumber(phoneNumber), correlationId);
        log.debug("Validating with OTP input: {} [correlationId={}]", 
                otpInput, correlationId);

        ECKey devicePub = SecurityUtils.extractDeviceJwkFromContext();
        // 1. Find the OTP request by public key hash
        String publicKeyHash = computeHash(devicePub.toJSONString());
        OtpEntity otpEntity = findOtpRequestByHash(publicKeyHash);

        // 2. Check if OTP is expired
        checkOtpExpiration(otpEntity);

        // 3. Create OTP data POJO for validation
        OtpData otpData = createOtpDataForValidation(phoneNumber, devicePub, otpInput);

        // 4. Verify OTP hash
        // Inline the DTO response logic here:
        boolean valid;
        String message;
        String details = null;
        try {
            String otpJSON = objectMapper.writeValueAsString(otpData);
            String canonicalJson = new JsonCanonicalizer(otpJSON).getEncodedString();

            if (log.isDebugEnabled()) {
                log.debug("OTP validation input: {}", canonicalJson);
            }

            if (passwordEncoder.matches(canonicalJson, otpEntity.getOtpHash())) {
                otpEntity.setStatus(OtpStatus.COMPLETE);
                otpRequestRepository.save(otpEntity);
                valid = true;
                message = "OTP validated successfully";
            } else {
                otpEntity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpEntity);
                valid = false;
                message = "Invalid OTP";
            }
        } catch (IOException e) {
            log.error("Failed to serialize OTP validation data to JSON", e);
            otpEntity.setStatus(OtpStatus.INCOMPLETE);
            otpRequestRepository.save(otpEntity);
            valid = false;
            message = "Error processing OTP data";
            details = e.getMessage();
        }
        OtpValidationResponse response = new OtpValidationResponse();
        response.setValid(valid);
        response.setMessage(message);
        response.setDetails(details);
        return response;
    }

    /**
     * Find OTP request by public key hash
     */
    private OtpEntity findOtpRequestByHash(String publicKeyHash) {
        return otpRequestRepository.findEntityByPublicKeyHash(publicKeyHash)
                .orElseThrow(() -> new ValidationException("No OTP request found for this public key"));
    }

    /**
     * Check if OTP is expired (5 minutes validity)
     */
    private void checkOtpExpiration(OtpEntity otpEntity) {
        if (otpEntity.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            log.warn("OTP expired for id: {}", otpEntity.getId());
            otpEntity.setStatus(OtpStatus.INCOMPLETE); // Using INCOMPLETE as there's no EXPIRED status
            otpRequestRepository.save(otpEntity);
            throw new ValidationException("OTP expired. Request a new one.");
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
            throw new ValidationException("Error computing hash");
        }
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