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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    @Resource
    private JavaMailSender mailSender;

    @Value("${otp.salt}")
    private String salt;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailOtpServiceImpl(PersonalInfoRepository personalInfoRepository, PasswordEncoder passwordEncoder) {
        this.personalInfoRepository = personalInfoRepository;
        this.passwordEncoder = passwordEncoder;
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
    public String sendEmailOtp(String accountId, String email) {
        log.info("Initiating OTP send process for email: {}, accountId: {}", email, accountId);
        validateEmailFormat(email);

        if (accountId == null || accountId.trim().isEmpty()) {
            log.error("Invalid accountId: {}", accountId);
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }

        try {
            String otp = generateOtp();
            Optional<PersonalInfoEntity> personalInfoOpt = personalInfoRepository.findByAccountId(accountId);
            PersonalInfoEntity personalInfo;

            if (personalInfoOpt.isEmpty()) {
                log.info("No user found for accountId: {}. Creating new record.", accountId);
                personalInfo = PersonalInfoEntity.builder()
                        .accountId(accountId)
                        .build();
            } else {
                personalInfo = personalInfoOpt.get();
                log.debug("Existing PersonalInfoEntity found: {}", personalInfo);
            }

            LocalDateTime otpExpiration = LocalDateTime.now().plusMinutes(5);
            personalInfo.setEmailOtpCode(otp);
            personalInfo.setEmailOtpHash(computeOtpHash(otp, accountId));
            personalInfo.setOtpExpirationDateTime(otpExpiration);

            personalInfoRepository.save(personalInfo);
            log.debug("PersonalInfoEntity saved: {}", personalInfo);

            sendOtpEmail(email, otp);
            return "OTP sent successfully to " + email;
        } catch (Exception e) {
            log.error("Failed to send OTP to {} for accountId: {}", email, accountId, e);
            throw new FailedToSendOTPException("Failed to send Webank email OTP: " + e.getMessage());
        }
    }

    @Override
    public String validateEmailOtp(String email, String otpInput, String accountId) {
        log.info("Validating OTP for email: {}, accountId: {}", email, accountId);
        try {
            PersonalInfoEntity personalInfo = personalInfoRepository.findByAccountId(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("User record not found"));

            validateOtpExpiration(personalInfo);

            if (validateOtpHash(otpInput, accountId, personalInfo)) {
                personalInfo.setEmail(email);
                personalInfoRepository.save(personalInfo);
                log.info("OTP verified successfully for email: {}", email);
                return "Webank email verified successfully";
            }

            personalInfoRepository.save(personalInfo);
            log.warn("Invalid OTP entered for email: {}", email);
            return "Invalid Webank OTP";
        } catch (IllegalArgumentException e) {
            log.error("Validation error for accountId: {}: {}", accountId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error validating OTP for accountId: {}", accountId, e);
            return "Webank OTP validation error";
        }
    }

    private void validateOtpExpiration(PersonalInfoEntity personalInfo) {
        LocalDateTime expiration = personalInfo.getOtpExpirationDateTime();

        if (expiration == null) {
            log.error("No OTP expiration date found for accountId: {}", personalInfo.getAccountId());
            throw new IllegalArgumentException("OTP expiration date missing");
        }

        if (LocalDateTime.now().isAfter(expiration)) {
            personalInfoRepository.save(personalInfo);
            log.warn("OTP expired for accountId: {}", personalInfo.getAccountId());
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
            throw new FailedToSendOTPException("Failed to send Webank email with attachment: " + e.getMessage());
        }
    }

    private boolean validateOtpHash(String inputOtp, String accountId, PersonalInfoEntity personalInfo) {
        log.debug("Validating OTP hash for input OTP");
        String otpPayload = generateOtpPayload(inputOtp, accountId);
        boolean isValid = passwordEncoder.matches(otpPayload, personalInfo.getEmailOtpHash());
        log.debug("OTP hash validation result: {}", isValid);
        return isValid;
    }

    String computeOtpHash(String emailOtp, String accountId) {
        log.debug("Computing OTP hash using Argon2");
        String otpPayload = generateOtpPayload(emailOtp, accountId);
        return passwordEncoder.encode(otpPayload);
    }
    
    private String generateOtpPayload(String emailOtp, String accountId) {
        String input = String.format("{\"emailOtp\":\"%s\", \"accountId\":\"%s\", \"salt\":\"%s\"}",
                emailOtp, accountId, salt);
        return canonicalizeJson(input);
    }

    public String computeHash(String input) {
        log.debug("Using Argon2 password encoder for hash computation");
        return passwordEncoder.encode(input);
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