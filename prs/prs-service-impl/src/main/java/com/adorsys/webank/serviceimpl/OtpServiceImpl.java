package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.service.OtpServiceApi;
import com.nimbusds.jose.jwk.JWK;
import jakarta.transaction.Transactional;
import org.erdtman.jcs.JsonCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    private final OtpRequestRepository otpRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpServiceImpl(
            OtpRequestRepository otpRequestRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.otpRequestRepository = otpRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String generateOtp() {
        int otp = 10000 + secureRandom.nextInt(90000);
        return String.valueOf(otp);
    }

    @Override
    @Transactional
    public String sendOtp(JWK devicePub, String phoneNumber) {
        if (phoneNumber == null || !phoneNumber.matches("\\+?[1-9]\\d{1,14}")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        if (passwordEncoder == null) {
            log.error("PasswordEncoder is null - check Spring Security configuration");
            throw new IllegalStateException("PasswordEncoder not configured properly");
        }

        try {
            String otp = generateOtp();
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);

            int updatedRows = otpRequestRepository.updateOtpByPublicKeyHash(
                    publicKeyHash,
                    otp,
                    OtpStatus.PENDING,
                    LocalDateTime.now()
            );

            OtpEntity otpRequest;
            if (updatedRows == 0) {
                otpRequest = OtpEntity.builder()
                        .phoneNumber(phoneNumber)
                        .publicKeyHash(publicKeyHash)
                        .status(OtpStatus.PENDING)
                        .build();
            } else {
                otpRequest = otpRequestRepository.findByPublicKeyHash(publicKeyHash)
                        .orElseThrow(() -> new FailedToSendOTPException("Failed to fetch updated OTP record"));
            }

            // Canonicalize and Argon2-encode the OTP payload (no static salt!)
            String otpJson = String.format(
                    "{\"otp\":\"%s\",\"devicePub\":%s,\"phoneNumber\":\"%s\"}",
                    otp, devicePub.toJSONString(), phoneNumber
            );
            String canonical = new JsonCanonicalizer(otpJson).getEncodedString();
            String otpHash;
            try {
                otpHash = passwordEncoder.encode(canonical);
                if (otpHash == null || otpHash.isEmpty()) {
                    throw new IllegalStateException("PasswordEncoder returned empty hash");
                }
            } catch (Exception e) {
                log.error("Failed to encode OTP with Argon2: {}", e.getMessage(), e);
                throw new FailedToSendOTPException("Failed to secure OTP");
            }

            otpRequest.setOtpHash(otpHash);
            otpRequest.setOtpCode(otp);
            otpRequestRepository.save(otpRequest);

            log.info("OTP code for {} is {}", phoneNumber, otpRequest.getOtpCode());
            return otpHash;
        } catch (Exception e) {
            log.error("Failed to send OTP to {}", phoneNumber, e);
            throw new FailedToSendOTPException("Failed to send OTP");
        }
    }

    @Override
    public String computeHash(String input) {
        return "";
    }

    @Override
    public String validateOtp(String phoneNumber, JWK devicePub, String otpInput) {
        try {
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);

            Optional<OtpEntity> opt = otpRequestRepository.findByPublicKeyHash(publicKeyHash);
            if (opt.isEmpty()) {
                return "No OTP request found for this public key";
            }

            OtpEntity entity = opt.get();
            LocalDateTime now = LocalDateTime.now();
            if (entity.getCreatedAt().isBefore(now.minusMinutes(5))) {
                entity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(entity);
                return "OTP expired. Request a new one.";
            }

            String otpJson = String.format(
                    "{\"otp\":\"%s\",\"devicePub\":%s,\"phoneNumber\":\"%s\"}",
                    otpInput, devicePub.toJSONString(), phoneNumber
            );
            String canonical = new JsonCanonicalizer(otpJson).getEncodedString();
            log.debug("Validating canonical OTP payload: {}", canonical);

            if (entity.getOtpHash() == null) {
                log.error("Stored OTP hash is null for publicKeyHash: {}", publicKeyHash);
                entity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(entity);
                return "Invalid OTP configuration";
            }
            
            boolean matches;
            try {
                matches = passwordEncoder.matches(canonical, entity.getOtpHash());
            } catch (Exception e) {
                log.error("Error during password matching: {}", e.getMessage(), e);
                entity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(entity);
                return "Error validating OTP";
            }
            
            if (matches) {
                entity.setStatus(OtpStatus.COMPLETE);
                otpRequestRepository.save(entity);
                return "Otp Validated Successfully";
            } else {
                entity.setStatus(OtpStatus.INCOMPLETE);
                otpRequestRepository.save(entity);
                return "Invalid OTP";
            }
        } catch (Exception e) {
            log.error("Error validating OTP", e);
            return "Error validating the OTP";
        }
    }

    /**
     * We still use SHA-256 to fingerprint the public key; this is separate from Argon2.
     */
    private String computePublicKeyHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new HashComputationException("Error computing public key hash");
        }
    }
}
