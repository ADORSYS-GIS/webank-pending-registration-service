package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import com.nimbusds.jose.jwk.*;
import org.erdtman.jcs.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.*;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
import  jakarta.mail.internet.MimeMessage;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class EmailOtpServiceImpl implements EmailOtpServiceApi {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpServiceImpl.class);
    private final PersonalInfoRepository personalInfoRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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
        log.debug("Generating new OTP");
        SecureRandom secureRandom = new SecureRandom();
        int otp = 100000 + secureRandom.nextInt(900000);
        log.debug("Generated OTP: {}", otp);
        return String.valueOf(otp);
    }

    @Override
    public String sendEmailOtp(JWK devicePub, String email) {
        log.info("Initiating OTP send process for email: {}", email);
        validateEmailFormat(email);

        try {
            String otp = generateOtp();
            String devicePublicKey = devicePub.toJSONString();
            log.debug("Device public key: {}", devicePublicKey);

            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            log.debug("Computed public key hash: {}", publicKeyHash);

            PersonalInfoEntity personalInfo = personalInfoRepository.findByPublicKeyHash(publicKeyHash)
                    .orElseThrow(() -> new IllegalArgumentException("User record not found"));


            LocalDateTime otpExpiration = LocalDateTime.now().plusMinutes(5);

            personalInfo.setEmailOtpCode(otp);
            personalInfo.setEmailOtpHash(computeOtpHash(otp, devicePublicKey));
            personalInfo.setOtpExpirationDateTime(otpExpiration);
            personalInfoRepository.save(personalInfo);

            log.debug("OTP stored successfully for public key hash: {}", publicKeyHash);
            sendOtpEmail(email, otp);
            return "OTP sent successfully to " + email;
        } catch (Exception e) {
            log.error("Failed to send OTP to {}", email, e);
            throw new FailedToSendOTPException("Failed to send Webank email OTP");
        }
    }

    @Override
    public String validateEmailOtp(String email, JWK devicePub, String otpInput) {
        log.info("Validating OTP for email: {}", email);
        try {
            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);
            log.debug("Computed public key hash: {}", publicKeyHash);

            PersonalInfoEntity personalInfo = personalInfoRepository.findByPublicKeyHash(publicKeyHash)
                    .orElseThrow(() -> new IllegalArgumentException("User record not found"));

            validateOtpExpiration(personalInfo);

            if (validateOtpHash(otpInput, devicePublicKey, personalInfo)) {
                personalInfo.setEmail(email);
                personalInfoRepository.save(personalInfo);
                log.info("OTP verified successfully for email: {}", email);
                return "Webank email verified successfully";
            }

            personalInfoRepository.save(personalInfo);
            log.warn("Invalid OTP entered for email: {}", email);
            return "Invalid Webank OTP";
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error validating OTP", e);
            return "Webank OTP validation error";
        }
    }

    private void validateOtpExpiration(PersonalInfoEntity personalInfo) {
        LocalDateTime expiration = personalInfo.getOtpExpirationDateTime();

        if (expiration == null) {
            log.error("No OTP expiration date found");
            throw new IllegalArgumentException("OTP expiration date missing");
        }

        if (LocalDateTime.now().isAfter(expiration)) {
            personalInfoRepository.save(personalInfo);
            log.warn("OTP expired for public key hash: {}", personalInfo.getPublicKeyHash());
            throw new IllegalArgumentException("Webank OTP expired");
        }
    }


    private void validateEmailFormat(String email) {
        log.debug("Validating email format for: {}", email);
        if (!email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            log.warn("Invalid email format: {}", email);
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private void sendOtpEmail(String toEmail, String otp) {
        log.info("Sending OTP email from {} to {}", fromEmail, toEmail);
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Webank Verification Code");
            helper.setText(String.format("Your Webank OTP is: %s (valid for 5 minutes)", otp));

            ByteArrayResource resource = new ByteArrayResource("This is a sample attachment".getBytes());
            helper.addAttachment("webank_otp_info.txt", resource);

            mailSender.send(message);
            log.info("OTP email sent successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", toEmail, e);
            throw new FailedToSendOTPException("Failed to send Webank email with attachment");
        }
    }

    private boolean validateOtpHash(String inputOtp, String devicePublicKey, PersonalInfoEntity personalInfo) {
        log.debug("Validating OTP hash for input OTP");
        String currentHash = computeOtpHash(inputOtp, devicePublicKey);
        boolean isValid = currentHash.equals(personalInfo.getEmailOtpHash());
        log.debug("OTP hash validation result: {}", isValid);
        return isValid;
    }

    String computeOtpHash(String emailOtp, String devicePublicKey) {
        log.debug("Computing OTP hash");
        String input = String.format("{\"emailOtp\":\"%s\", \"devicePub\":%s, \"salt\":\"%s\"}",
                emailOtp, devicePublicKey, salt);
        log.trace("Hash input: {}", input);
        return computeHash(canonicalizeJson(input));
    }

    private String computePublicKeyHash(String devicePublicKey) {
        log.debug("Computing public key hash");
        return computeHash(devicePublicKey);
    }




    public String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new HashComputationException("Error computing hash");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }


    String canonicalizeJson(String json) {
        try {
            log.trace("Canonicalizing JSON: {}", json);
            JsonCanonicalizer jc = new JsonCanonicalizer(json);
            String canonicalJson = jc.getEncodedString();
            log.trace("Canonicalized JSON: {}", canonicalJson);
            return canonicalJson;
        } catch (Exception e) {
            log.error("JSON canonicalization failed", e);
            throw new HashComputationException("Webank JSON canonicalization error");
        }
    }
}