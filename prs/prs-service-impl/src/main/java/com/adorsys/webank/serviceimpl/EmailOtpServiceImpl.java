package com.adorsys.webank.serviceimpl;

import com.adorsys.error.ValidationException;
import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.model.EmailOtpData;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.EmailOtpServiceApi;
import com.adorsys.webank.serviceimpl.helper.MailHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.erdtman.jcs.JsonCanonicalizer;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOtpServiceImpl implements EmailOtpServiceApi {

    private final PersonalInfoRepository personalInfoRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailHelper mailHelper;

    // Constants
    private static final int OTP_EXPIRATION_MINUTES = 5;
    private static final String EMAIL_REGEX = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

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
                mailHelper.maskAccountId(accountId), correlationId);
        log.debug("Target email: {} [correlationId={}]", mailHelper.maskEmail(email), correlationId);
        
        validateEmailFormat(email);

        if (accountId == null || accountId.trim().isEmpty()) {
            log.error("Invalid accountId provided: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), correlationId);
            throw new ValidationException("Account ID cannot be null or empty");
        }

        try {
            String otp = generateOtp();
            Optional<PersonalInfoProjection> personalInfoOpt = personalInfoRepository.findByAccountId(accountId);
            PersonalInfoEntity personalInfo;

            if (personalInfoOpt.isEmpty()) {
                log.info("No user found for account: {}. Creating new record. [correlationId={}]",
                        mailHelper.maskAccountId(accountId), correlationId);
                personalInfo = PersonalInfoEntity.builder()
                        .accountId(accountId)
                        .build();
            } else {
                personalInfo = new PersonalInfoEntity();
                personalInfo.setAccountId(accountId);
                log.debug("Existing record found for account: {} [correlationId={}]",
                        mailHelper.maskAccountId(accountId), correlationId);
            }

            LocalDateTime otpExpiration = LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES);
            personalInfo.setEmailOtpCode(otp);
            personalInfo.setEmailOtpHash(computeOtpHash(otp, accountId));
            personalInfo.setOtpExpirationDateTime(otpExpiration);

            personalInfoRepository.save(personalInfo);
            log.debug("OTP record saved for account: {} with expiration: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), otpExpiration, correlationId);

            mailHelper.sendOtpEmail(email, otp);
            log.info("Email OTP sent successfully to account: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), correlationId);
            return "OTP sent successfully to " + email;
        } catch (Exception e) {
            log.error("Failed to send Email OTP to account: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), correlationId, e);
            throw new ValidationException("Failed to send Webank email OTP: " + e.getMessage());
        }
    }

    @Override
    public String validateEmailOtp(String email, String otpInput, String accountId) {
        String correlationId = MDC.get("correlationId");
        log.info("Validating Email OTP for account: {} [correlationId={}]",
                mailHelper.maskAccountId(accountId), correlationId);
        log.debug("Email being validated: {} [correlationId={}]", mailHelper.maskEmail(email), correlationId);
        
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
                        mailHelper.maskAccountId(accountId), correlationId);
                return "Webank email verified successfully";
            }

            log.warn("Invalid Email OTP entered for account: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), correlationId);
            return "Invalid Webank OTP";
        } catch (ValidationException e) {
            log.error("Validation error for account: {}: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), e.getMessage(), correlationId);
            throw e;
        } catch (Exception e) {
            log.error("Error validating Email OTP for account: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), correlationId, e);
            throw new ValidationException("Failed to validate OTP: " + e.getMessage());
        }
    }

    private void validateOtpInput(String otpInput, String accountId) {
        String correlationId = MDC.get("correlationId");
        if (otpInput == null || otpInput.trim().isEmpty()) {
            log.warn("Empty OTP provided for accountId: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), correlationId);
            throw new ValidationException("OTP cannot be empty");
        }
    }

    private PersonalInfoProjection getPersonalInfoProjection(String accountId) {
        String correlationId = MDC.get("correlationId");
        return personalInfoRepository.findByAccountId(accountId)
            .orElseThrow(() -> {
                log.warn("User record not found for account: {} [correlationId={}]",
                        mailHelper.maskAccountId(accountId), correlationId);
                return new ValidationException("User record not found");
            });
    }

    private void validateOtpExpiration(LocalDateTime expiration, String accountId) {
        String correlationId = MDC.get("correlationId");
        if (expiration == null) {
            log.error("No OTP expiration date found for account: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), correlationId);
            throw new ValidationException("OTP expiration date missing");
        }
        if (LocalDateTime.now().isAfter(expiration)) {
            log.warn("Email OTP expired for account: {}, expired at: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), expiration, correlationId);
            throw new ValidationException("OTP has expired. Please request a new one.");
        }
        
        log.debug("Email OTP expiration valid for account: {}, expires at: {} [correlationId={}]",
                mailHelper.maskAccountId(accountId), expiration, correlationId);
    }

    private void validateStoredOtp(String storedOtpCode, String accountId) {
        String correlationId = MDC.get("correlationId");
        if (storedOtpCode == null || storedOtpCode.trim().isEmpty()) {
            log.warn("No OTP code found for accountId: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), correlationId);
            throw new ValidationException("No OTP code found. Please request a new one.");
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
        log.debug("Validating email format: {} [correlationId={}]", mailHelper.maskEmail(email), correlationId);
        if (!email.matches(EMAIL_REGEX)) {
            log.warn("Invalid email format provided: {} [correlationId={}]",
                    mailHelper.maskEmail(email), correlationId);
            throw new ValidationException("Invalid email format");
        }
        log.debug("Email format validation successful [correlationId={}]", correlationId);
    }

    String computeOtpHash(String emailOtp, String accountId) {
        String correlationId = MDC.get("correlationId");
        log.debug("Computing OTP hash for account: {} [correlationId={}]",
                mailHelper.maskAccountId(accountId), correlationId);
        try {
            EmailOtpData otpData = EmailOtpData.create(emailOtp, accountId);
            String input = objectMapper.writeValueAsString(otpData);
            log.trace("Hash input: {}", input);
            return passwordEncoder.encode(canonicalizeJson(input));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OTP hash data", e);
            throw new ValidationException("Failed to compute OTP hash: " + e.getMessage());
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
            throw new ValidationException("Error computing hash");
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
            throw new ValidationException("Error canonicalizing JSON: " + e.getMessage());
        }
    }
}