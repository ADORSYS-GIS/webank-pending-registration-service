package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.exceptions.AccountNotFoundException;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.model.EmailOtpData;
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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

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
    @Transactional
    public String sendEmailOtp(String accountId, String email) {
        String correlationId = MDC.get("correlationId");
        log.info("Initiating Email OTP send process for account: {} [correlationId={}]",
                mailHelper.maskAccountId(accountId), correlationId);
        log.debug("Target email: {} [correlationId={}]", mailHelper.maskEmail(email), correlationId);
        
        validateEmailFormat(email);

        if (accountId == null || accountId.trim().isEmpty()) {
            log.error("Invalid accountId provided: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), correlationId);
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }

        try {
            String otp = generateOtp();
            PersonalInfoEntity personalInfo = personalInfoRepository.findById(accountId)
                    .orElseThrow(() -> new AccountNotFoundException("No user found for account: " + accountId));

            LocalDateTime otpExpiration = LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES);
            personalInfo.setEmail(email); // Set the main email field
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
        } catch (AccountNotFoundException e) {
            log.warn("Attempted to send OTP to a non-existent account: {} [correlationId={}]", mailHelper.maskAccountId(accountId), MDC.get("correlationId"));
            throw e;
        } catch (Exception e) {
            log.error("Failed to send Email OTP to account: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), correlationId, e);
            throw new FailedToSendOTPException("Failed to send Webank email OTP: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String validateEmailOtp(String email, String otp, String accountId) {
        String correlationId = MDC.get("correlationId");
        log.info("Validating Email OTP for account: {} [correlationId={}]", mailHelper.maskAccountId(accountId), correlationId);

        try {
            PersonalInfoEntity entity = getPersonalInfo(accountId);
            validateOtp(otp, entity);
            clearOtpFields(entity);

            log.info("Email OTP verified successfully for account: {} [correlationId={}]", mailHelper.maskAccountId(accountId), correlationId);
            return "Webank email verified successfully";

        } catch (IllegalArgumentException e) {
            log.error("Validation error for account: {}: {} [correlationId={}]", mailHelper.maskAccountId(accountId), e.getMessage(), correlationId);
            throw e;
        }
    }

    private PersonalInfoEntity getPersonalInfo(String accountId) {
        String correlationId = MDC.get("correlationId");
        return personalInfoRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("User record not found for account: {} [correlationId={}]",
                            mailHelper.maskAccountId(accountId), correlationId);
                    return new IllegalArgumentException("User record not found");
                });
    }

    private void validateOtp(String otp, PersonalInfoEntity entity) {
        validateOtpExpiration(entity.getOtpExpirationDateTime(), entity.getAccountId());

        String otpHash = entity.getEmailOtpHash();
        if (otpHash == null || otpHash.isEmpty()) {
            log.warn("OTP hash not found for account: {} [correlationId={}]", mailHelper.maskAccountId(entity.getAccountId()), MDC.get("correlationId"));
            throw new IllegalArgumentException("No OTP has been generated for this account.");
        }

        String rawData = computeRawData(otp, entity.getAccountId());
        if (!passwordEncoder.matches(rawData, otpHash)) {
            log.warn("Invalid OTP provided for account: {} [correlationId={}]", mailHelper.maskAccountId(entity.getAccountId()), MDC.get("correlationId"));
            throw new IllegalArgumentException("Invalid OTP provided.");
        }
    }

    private void clearOtpFields(PersonalInfoEntity entity) {
        entity.setEmailOtpCode(null);
        entity.setEmailOtpHash(null);
        entity.setOtpExpirationDateTime(null);
        personalInfoRepository.save(entity);
    }

    private void validateOtpExpiration(LocalDateTime expirationDateTime, String accountId) {
        String correlationId = MDC.get("correlationId");
        if (expirationDateTime == null || expirationDateTime.isBefore(LocalDateTime.now())) {
            log.warn("Email OTP expired for account: {}, expired at: {} [correlationId={}]",
                    mailHelper.maskAccountId(accountId), expirationDateTime, correlationId);
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }
        
        log.debug("Email OTP expiration valid for account: {}, expires at: {} [correlationId={}]",
                mailHelper.maskAccountId(accountId), expirationDateTime, correlationId);
    }

    private String computeRawData(String otp, String accountId) {
        EmailOtpData otpData = EmailOtpData.create(otp, accountId);
        try {
            return canonicalizeJson(objectMapper.writeValueAsString(otpData));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OTP hash data", e);
            throw new HashComputationException("Failed to compute OTP hash: " + e.getMessage());
        }
    }

    private void validateEmailFormat(String email) {
        String correlationId = MDC.get("correlationId");
        log.debug("Validating email format: {} [correlationId={}]", mailHelper.maskEmail(email), correlationId);
        if (!email.matches(EMAIL_REGEX)) {
            log.warn("Invalid email format provided: {} [correlationId={}]",
                    mailHelper.maskEmail(email), correlationId);
            throw new IllegalArgumentException("Invalid email format");
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
}