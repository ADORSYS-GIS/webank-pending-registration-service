package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.service.OtpServiceApi;
import com.nimbusds.jose.jwk.JWK;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpServiceApi {
    private final OtpRequestRepository otpRequestRepository;
    private final PasswordHashingService passwordHashingService;


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
                    "{\"otp\":\"%s\",\"devicePub\":%s,\"phoneNumber\":\"%s\"}",
                    otp, devicePub.toJSONString(), phoneNumber
            );
            String canonicalJson = new JsonCanonicalizer(otpJSON).getEncodedString();
            String otpHash = passwordHashingService.hash(canonicalJson);

            // Set hash and save
            otpRequest.setOtpHash(otpHash);
            otpRequest.setOtpCode(otp);
            otpRequestRepository.save(otpRequest);

            log.info("OTP sent successfully to phone number: {}", phoneNumber);
            return otpHash;
        } catch (Exception e) {
            log.error("Failed to send OTP to {}", phoneNumber, e);
            throw new FailedToSendOTPException("Failed to send OTP");
        }
    }


    @Override
    public String validateOtp(String phoneNumber, JWK devicePub, String otpInput) {
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

            String otpJSON = String.format(
                    "{\"otp\":\"%s\",\"devicePub\":%s,\"phoneNumber\":\"%s\"}",
                    otpInput, devicePub.toJSONString(), phoneNumber
            );
            
            String canonicalJson = new JsonCanonicalizer(otpJSON).getEncodedString();
            
            if (log.isDebugEnabled()) {
                log.debug("OTP validation input: {}", canonicalJson);
            }

            if (passwordHashingService.verify(canonicalJson, otpEntity.getOtpHash())) {
                otpEntity.setStatus(OtpStatus.COMPLETE);
                otpRequestRepository.save(otpEntity);
                return "Otp Validated Successfully";
            } else {
                otpEntity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(otpEntity);
                return "Invalid OTP";
            }
        } catch (Exception e) {
            log.error("Error validating OTP", e);
            return "Error validating the OTP";
        }
    }

    @Override
    public String computeHash(String input) {
        return passwordHashingService.hash(input);
    }

    private String computePublicKeyHash(String devicePublicKey) {
        return passwordHashingService.hash(devicePublicKey);
    }

}