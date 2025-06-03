package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.repository.*;
import com.adorsys.webank.service.*;
import jakarta.annotation.Resource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.erdtman.jcs.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.*;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
import com.adorsys.webank.domain.PersonalInfoStatus;
import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.time.format.*;
import java.util.Optional;

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
        log.debug("Generating new Email OTP");
        SecureRandom secureRandom = new SecureRandom();
        int otp = 100000 + secureRandom.nextInt(900000);
        log.debug("Email OTP generated successfully");
        return String.valueOf(otp);
    }

    @Override
    public String sendEmailOtp(String accountId, String email) {
        log.info("Initiating Email OTP send process for account: {}", maskAccountId(accountId));
        log.debug("Target email: {}", maskEmail(email));
        
        validateEmailFormat(email);

        if (accountId == null || accountId.trim().isEmpty()) {
            log.error("Invalid accountId provided: {}", maskAccountId(accountId));
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }

        try {
            String otp = generateOtp();
            Optional<PersonalInfoEntity> personalInfoOpt = personalInfoRepository.findByAccountId(accountId);
            PersonalInfoEntity personalInfo;

            if (personalInfoOpt.isEmpty()) {
                log.info("No user found for account: {}. Creating new record.", maskAccountId(accountId));
                personalInfo = PersonalInfoEntity.builder()
                        .accountId(accountId)
                        .build();
            } else {
                personalInfo = personalInfoOpt.get();
                log.debug("Existing record found for account: {}", maskAccountId(accountId));
            }

            LocalDateTime otpExpiration = LocalDateTime.now().plusMinutes(5);
            personalInfo.setEmailOtpCode(otp);
            personalInfo.setEmailOtpHash(computeOtpHash(otp, accountId));
            personalInfo.setOtpExpirationDateTime(otpExpiration);

            personalInfoRepository.save(personalInfo);
            log.debug("OTP record saved for account: {} with expiration: {}", 
                     maskAccountId(accountId), otpExpiration);

            sendOtpEmail(email, otp);
            log.info("Email OTP sent successfully to account: {}", maskAccountId(accountId));
            return "OTP sent successfully to " + email;
        } catch (Exception e) {
            log.error("Failed to send Email OTP to account: {}", maskAccountId(accountId), e);
            throw new FailedToSendOTPException("Failed to send Webank email OTP: " + e.getMessage());
        }
    }

    @Override
    public String validateEmailOtp(String email, String otpInput, String accountId) {
        log.info("Validating Email OTP for account: {}", maskAccountId(accountId));
        log.debug("Email being validated: {}", maskEmail(email));
        
        try {
            PersonalInfoEntity personalInfo = personalInfoRepository.findByAccountId(accountId)
                    .orElseThrow(() -> {
                        log.warn("User record not found for account: {}", maskAccountId(accountId));
                        return new IllegalArgumentException("User record not found");
                    });

            validateOtpExpiration(personalInfo);

            if (validateOtpHash(otpInput, accountId, personalInfo)) {
                personalInfo.setEmail(email);
                personalInfoRepository.save(personalInfo);
                log.info("Email OTP verified successfully for account: {}", maskAccountId(accountId));
                return "Webank email verified successfully";
            }

            personalInfoRepository.save(personalInfo);
            log.warn("Invalid Email OTP entered for account: {}", maskAccountId(accountId));
            return "Invalid Webank OTP";
        } catch (IllegalArgumentException e) {
            log.error("Validation error for account: {}: {}", maskAccountId(accountId), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error validating Email OTP for account: {}", maskAccountId(accountId), e);
            return "Webank OTP validation error";
        }
    }

    private void validateOtpExpiration(PersonalInfoEntity personalInfo) {
        LocalDateTime expiration = personalInfo.getOtpExpirationDateTime();
        String accountId = personalInfo.getAccountId();

        if (expiration == null) {
            log.error("No OTP expiration date found for account: {}", maskAccountId(accountId));
            throw new IllegalArgumentException("OTP expiration date missing");
        }

        if (LocalDateTime.now().isAfter(expiration)) {
            personalInfoRepository.save(personalInfo);
            log.warn("Email OTP expired for account: {}, expired at: {}", 
                    maskAccountId(accountId), expiration);
            throw new IllegalArgumentException("Webank OTP expired");
        }
        
        log.debug("Email OTP expiration valid for account: {}, expires at: {}", 
                maskAccountId(accountId), expiration);
    }

    private void validateEmailFormat(String email) {
        log.debug("Validating email format: {}", maskEmail(email));
        if (!email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            log.warn("Invalid email format provided: {}", maskEmail(email));
            throw new IllegalArgumentException("Invalid email format");
        }
        log.debug("Email format validation successful");
    }

    private void sendOtpEmail(String toEmail, String otp) {
        log.info("Sending Email OTP to: {}", maskEmail(toEmail));
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
            log.info("Email OTP sent successfully to: {}", maskEmail(toEmail));
        } catch (MessagingException e) {
            log.error("Failed to send Email OTP to: {}", maskEmail(toEmail), e);
            throw new FailedToSendOTPException("Failed to send Webank email with attachment: " + e.getMessage());
        }
    }

    private boolean validateOtpHash(String inputOtp, String accountId, PersonalInfoEntity personalInfo) {
        log.debug("Validating OTP hash for account: {}", maskAccountId(accountId));
        String currentHash = computeOtpHash(inputOtp, accountId);
        boolean isValid = currentHash.equals(personalInfo.getEmailOtpHash());
        log.debug("OTP hash validation result for account {}: {}", maskAccountId(accountId), isValid);
        return isValid;
    }

    String computeOtpHash(String emailOtp, String accountId) {
        log.debug("Computing OTP hash for account: {}", maskAccountId(accountId));
        String input = String.format("{\"emailOtp\":\"%s\", \"accountId\":\"%s\", \"salt\":\"%s\"}",
                emailOtp, accountId, salt);
        return computeHash(canonicalizeJson(input));
    }

    public String computeHash(String input) {
        try {
            log.debug("Computing hash for input");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String result = bytesToHex(hashBytes);
            log.debug("Hash computation completed");
            return result;
        } catch (NoSuchAlgorithmException e) {
            log.error("Error computing hash", e);
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
        try {
            JsonCanonicalizer jc = new JsonCanonicalizer(jsonString);
            return jc.getEncodedString();
        } catch (Exception e) {
            log.error("Error canonicalizing JSON", e);
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
        
        return email.substring(0, 1) + "********";
    }
}