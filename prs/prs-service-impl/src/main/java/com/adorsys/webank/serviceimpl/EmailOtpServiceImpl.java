package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import com.nimbusds.jose.jwk.*;
import org.erdtman.jcs.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.mail.*;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.*;

import jakarta.annotation.Resource;
import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class EmailOtpServiceImpl implements EmailOtpServiceApi {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpServiceImpl.class);
    private final PersonalInfoRepository personalInfoRepository;

    @Resource
    private JavaMailSender mailSender;

    @Value("${otp.salt}")
    private String salt;
    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailOtpServiceImpl(PersonalInfoRepository personalInfoRepository) {
        this.personalInfoRepository = personalInfoRepository;
    }

    @Override
    public String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int otp = 100000 + secureRandom.nextInt(900000); // 6-digit OTP
        log.debug("Generated OTP: {}", otp);
        return String.valueOf(otp);
    }

    @Override
    public String sendEmailOtp(JWK devicePub, String email) {
        log.debug("Initiating OTP sending process for email: {}", email);
        validateEmailFormat(email);

        try {
            String otp = generateOtp();
            String devicePublicKey = devicePub.toJSONString();
            log.debug("Device public key: {}", devicePublicKey);

            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            log.debug("Computed public key hash: {}", publicKeyHash);

            PersonalInfoEntity personalInfo = personalInfoRepository.findByPublicKeyHash(publicKeyHash)
                    .orElseThrow(() -> new IllegalArgumentException("User record not found"));

            personalInfo.setEmail(email);
            personalInfo.setEmailOtpCode(otp);
            personalInfo.setEmailOtpHash(computeOtpHash(otp, devicePublicKey));
            personalInfo.setExpirationDate(LocalDateTime.now().plusMinutes(5));
            personalInfo.setStatus(PersonalInfoStatus.PENDING);
            personalInfoRepository.save(personalInfo);

            log.debug("OTP stored successfully for email: {}", email);
            sendOtpEmail(email, otp);
            return "OTP sent successfully";
        } catch (Exception e) {
            log.error("Failed to send OTP", e);
            throw new FailedToSendOTPException("Failed to send email OTP");
        }
    }

    @Override
    public String validateEmailOtp(String email, JWK devicePub, String otpInput) {
        log.debug("Validating OTP for email: {}", email);
        try {
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            log.debug("Computed public key hash: {}", publicKeyHash);

            PersonalInfoEntity personalInfo = personalInfoRepository.findByPublicKeyHash(publicKeyHash)
                    .orElseThrow(() -> new IllegalArgumentException("User record not found"));

            validateOtpExpiration(personalInfo);

            if (validateOtpHash(otpInput, devicePublicKey, personalInfo)) {
                personalInfo.setStatus(PersonalInfoStatus.VERIFIED);
                personalInfoRepository.save(personalInfo);
                log.debug("OTP verified successfully for email: {}", email);
                return "Email verified successfully";
            }

            personalInfo.setStatus(PersonalInfoStatus.FAILED);
            personalInfoRepository.save(personalInfo);
            log.debug("Invalid OTP entered for email: {}", email);
            return "Invalid OTP";
        } catch (Exception e) {
            log.error("Error validating OTP", e);
            return "Error validating OTP";
        }
    }

    // Helper methods
    private void validateEmailFormat(String email) {
        if (!email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            log.warn("Invalid email format: {}", email);
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private void sendOtpEmail(String toEmail, String otp) {
        log.debug("Sending OTP email to: {}", toEmail);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your Verification Code");
        message.setText("Your OTP is: " + otp + " (valid for 5 minutes)");
        mailSender.send(message);
        log.debug("OTP email sent successfully to: {}", toEmail);
    }

    private boolean validateOtpHash(String inputOtp, String devicePublicKey, PersonalInfoEntity personalInfo) {
        String currentHash = computeOtpHash(inputOtp, devicePublicKey);
        boolean isValid = currentHash.equals(personalInfo.getEmailOtpHash());
        log.debug("OTP hash validation result: {}", isValid);
        return isValid;
    }

    private void validateOtpExpiration(PersonalInfoEntity personalInfo) {
        if (LocalDateTime.now().isAfter(personalInfo.getExpirationDate())) {
            personalInfo.setStatus(PersonalInfoStatus.EXPIRED);
            personalInfoRepository.save(personalInfo);
            log.warn("OTP expired for email: {}", personalInfo.getEmail());
            throw new IllegalArgumentException("OTP expired");
        }
    }

    private String computeOtpHash(String emailOtp, String devicePublicKey) {
        String input = String.format("{\"emailOtp\":\"%s\", \"devicePub\":%s, \"salt\":\"%s\"}",
                emailOtp, devicePublicKey, salt);
        log.debug("Computing OTP hash with input: {}", input);
        return computeHash(canonicalizeJson(input));
    }

    private String computePublicKeyHash(String devicePublicKey) {
        log.debug("Computing public key hash for: {}", devicePublicKey);
        return computeHash(devicePublicKey);
    }

    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hash = Base64.getEncoder().encodeToString(hashBytes);
            log.debug("Computed hash: {}", hash);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            log.error("Error computing hash", e);
            throw new HashComputationException("Error computing hash");
        }
    }

    private String canonicalizeJson(String json) {
        try {
            JsonCanonicalizer jc = new JsonCanonicalizer(json);
            String canonicalJson = jc.getEncodedString();
            log.debug("Canonicalized JSON: {}", canonicalJson);
            return canonicalJson;
        } catch (Exception e) {
            log.error("Error canonicalizing JSON", e);
            throw new HashComputationException("Error canonicalizing JSON");
        }
    }
}
