package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.model.EmailOtpData;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.EmailOtpServiceApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.erdtman.jcs.JsonCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EmailOtpServiceImpl implements EmailOtpServiceApi {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpServiceImpl.class);
    private final PersonalInfoRepository personalInfoRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    // Constants
    private static final int OTP_EXPIRATION_MINUTES = 5;
    private static final String EMAIL_REGEX = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailOtpServiceImpl(PersonalInfoRepository personalInfoRepository, ObjectMapper objectMapper, PasswordEncoder passwordEncoder) {
        this.personalInfoRepository = personalInfoRepository;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String generateOtp() {
        String correlationId = MDC.get("correlationId");
        log.debug("Generating new Email OTP [correlationId={}]", correlationId);
        SecureRandom secureRandom = new SecureRandom();
        int otp = 100000 + secureRandom.nextInt(900000);
        log.debug("Email OTP generated successfully [correlationId={}]", correlationId);
        return String.valueOf(otp);
    }

    @Override
    public String sendEmailOtp(String accountId, String email) {
        String correlationId = MDC.get("correlationId");
        log.info("Initiating Email OTP send process for account: {} [correlationId={}]", 
                maskAccountId(accountId), correlationId);
        log.debug("Target email: {} [correlationId={}]", maskEmail(email), correlationId);
        
        validateEmailFormat(email);

        if (accountId == null || accountId.trim().isEmpty()) {
            log.error("Invalid accountId provided: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }

        try {
            String otp = generateOtp();
            Optional<PersonalInfoProjection> personalInfoOpt = personalInfoRepository.findByAccountId(accountId);
            PersonalInfoEntity personalInfo;

            if (personalInfoOpt.isEmpty()) {
                log.info("No user found for account: {}. Creating new record. [correlationId={}]", 
                        maskAccountId(accountId), correlationId);
                personalInfo = PersonalInfoEntity.builder()
                        .accountId(accountId)
                        .build();
            } else {
                personalInfo = new PersonalInfoEntity();
                personalInfo.setAccountId(accountId);
                log.debug("Existing record found for account: {} [correlationId={}]", 
                        maskAccountId(accountId), correlationId);
            }

            LocalDateTime otpExpiration = LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES);
            personalInfo.setEmailOtpCode(otp);
            personalInfo.setEmailOtpHash(computeOtpHash(otp, accountId));
            personalInfo.setOtpExpirationDateTime(otpExpiration);

            personalInfoRepository.save(personalInfo);
            log.debug("OTP record saved for account: {} with expiration: {} [correlationId={}]", 
                     maskAccountId(accountId), otpExpiration, correlationId);

            sendOtpEmail(email, otp);
            log.info("Email OTP sent successfully to account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            return "OTP sent successfully to " + email;
        } catch (Exception e) {
            log.error("Failed to send Email OTP to account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId, e);
            throw new FailedToSendOTPException("Failed to send Webank email OTP: " + e.getMessage());
        }
    }

    @Override
    public String validateEmailOtp(String email, String otpInput, String accountId) {
        String correlationId = MDC.get("correlationId");
        log.info("Validating Email OTP for account: {} [correlationId={}]", 
                maskAccountId(accountId), correlationId);
        log.debug("Email being validated: {} [correlationId={}]", maskEmail(email), correlationId);
        
        validateOtpInput(otpInput, accountId);
        
        try {
            PersonalInfoProjection projection = getPersonalInfoProjection(accountId);
            validateOtpExpiration(projection.getOtpExpirationDateTime(), accountId);
            validateStoredOtp(projection.getEmailOtpCode(), accountId);
            
            log.debug("Comparing OTPs - Stored: '{}', Input: '{}' [correlationId={}]", 
                    projection.getEmailOtpCode(), otpInput, correlationId);

            EmailOtpData otpData = EmailOtpData.create(otpInput, accountId);
            String input = objectMapper.writeValueAsString(otpData);

            boolean isValid = passwordEncoder.matches(canonicalizeJson(input), projection.getEmailOtpHash());
            if (isValid) {
                PersonalInfoEntity entity = createPersonalInfoEntity(accountId, email, projection);
                personalInfoRepository.save(entity);
                log.info("Email OTP verified successfully for account: {} [correlationId={}]", 
                        maskAccountId(accountId), correlationId);
                return "Webank email verified successfully";
            }

            log.warn("Invalid Email OTP entered for account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            return "Invalid Webank OTP";
        } catch (IllegalArgumentException e) {
            log.error("Validation error for account: {}: {} [correlationId={}]", 
                    maskAccountId(accountId), e.getMessage(), correlationId);
            throw e;
        } catch (Exception e) {
            log.error("Error validating Email OTP for account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId, e);
            throw new IllegalStateException("Failed to validate OTP: " + e.getMessage(), e);
        }
    }

    private void validateOtpInput(String otpInput, String accountId) {
        String correlationId = MDC.get("correlationId");
        if (otpInput == null || otpInput.trim().isEmpty()) {
            log.warn("Empty OTP provided for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            throw new IllegalArgumentException("OTP cannot be empty");
        }
    }

    private PersonalInfoProjection getPersonalInfoProjection(String accountId) {
        String correlationId = MDC.get("correlationId");
        return personalInfoRepository.findByAccountId(accountId)
            .orElseThrow(() -> {
                log.warn("User record not found for account: {} [correlationId={}]", 
                        maskAccountId(accountId), correlationId);
                return new IllegalArgumentException("User record not found");
            });
    }

    private void validateOtpExpiration(LocalDateTime expiration, String accountId) {
        String correlationId = MDC.get("correlationId");
        if (expiration == null) {
            log.error("No OTP expiration date found for account: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            throw new IllegalArgumentException("OTP expiration date missing");
        }
        if (LocalDateTime.now().isAfter(expiration)) {
            log.warn("Email OTP expired for account: {}, expired at: {} [correlationId={}]", 
                    maskAccountId(accountId), expiration, correlationId);
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }
        
        log.debug("Email OTP expiration valid for account: {}, expires at: {} [correlationId={}]", 
                maskAccountId(accountId), expiration, correlationId);
    }

    private void validateStoredOtp(String storedOtpCode, String accountId) {
        String correlationId = MDC.get("correlationId");
        if (storedOtpCode == null || storedOtpCode.trim().isEmpty()) {
            log.warn("No OTP code found for accountId: {} [correlationId={}]", 
                    maskAccountId(accountId), correlationId);
            throw new IllegalArgumentException("No OTP code found. Please request a new one.");
        }
    }

    private PersonalInfoEntity createPersonalInfoEntity(String accountId, String email, PersonalInfoProjection projection) {
        PersonalInfoEntity entity = new PersonalInfoEntity();
        entity.setAccountId(accountId);
        entity.setEmail(email);
        entity.setEmailOtpCode(projection.getEmailOtpCode());
        entity.setEmailOtpHash(projection.getEmailOtpHash());
        entity.setOtpExpirationDateTime(projection.getOtpExpirationDateTime());
        return entity;
    }

    private void validateEmailFormat(String email) {
        String correlationId = MDC.get("correlationId");
        log.debug("Validating email format: {} [correlationId={}]", maskEmail(email), correlationId);
        if (!email.matches(EMAIL_REGEX)) {
            log.warn("Invalid email format provided: {} [correlationId={}]", 
                    maskEmail(email), correlationId);
            throw new IllegalArgumentException("Invalid email format");
        }
        log.debug("Email format validation successful [correlationId={}]", correlationId);
    }

    private void sendOtpEmail(String toEmail, String otp) {
        String correlationId = MDC.get("correlationId");
        log.info("Sending Email OTP to: {} [correlationId={}]", maskEmail(toEmail), correlationId);
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
            log.info("Email OTP sent successfully to: {} [correlationId={}]", 
                    maskEmail(toEmail), correlationId);
        } catch (MessagingException e) {
            log.error("Failed to send Email OTP to: {} [correlationId={}]", 
                    maskEmail(toEmail), correlationId, e);
            throw new FailedToSendOTPException("Failed to send Webank email with attachment: " + e.getMessage());
        }
    }

    String computeOtpHash(String emailOtp, String accountId) {
        String correlationId = MDC.get("correlationId");
        log.debug("Computing OTP hash for account: {} [correlationId={}]", 
                maskAccountId(accountId), correlationId);
        try {
            EmailOtpData otpData = EmailOtpData.create(emailOtp, accountId);
            String input = objectMapper.writeValueAsString(otpData);
            log.trace("Hash input: {}", input);
            return passwordEncoder.encode(canonicalizeJson(input));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OTP hash data", e);
            throw new HashComputationException("Failed to compute OTP hash: " + e.getMessage());
        }
    }

    public String computeHash(String input) {
        String correlationId = MDC.get("correlationId");
        try {
            log.debug("Computing hash for input [correlationId={}]", correlationId);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String result = bytesToHex(hashBytes);
            log.debug("Hash computation completed [correlationId={}]", correlationId);
            return result;
        } catch (NoSuchAlgorithmException e) {
            log.error("Error computing hash [correlationId={}]", correlationId, e);
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

    String canonicalizeJson(String jsonString) {
        String correlationId = MDC.get("correlationId");
        try {
            JsonCanonicalizer jc = new JsonCanonicalizer(jsonString);
            return jc.getEncodedString();
        } catch (Exception e) {
            log.error("Error canonicalizing JSON [correlationId={}]", correlationId, e);
            throw new HashComputationException("Error canonicalizing JSON: " + e.getMessage());
        }
    }
    
    /**
     * Masks an account ID for logging purposes
     * Shows only first 2 and last 2 characters
     */
    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() < 5) {
            return "********";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
    
    /**
     * Masks an email address for logging purposes
     * Shows only first character and domain
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "********";
        }
        
        if (email.contains("@")) {
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                String firstChar = email.substring(0, 1);
                String domain = email.substring(atIndex);
                return firstChar + "****" + domain;
            }
        }
        
        return email.charAt(0) + "********";
    }
}