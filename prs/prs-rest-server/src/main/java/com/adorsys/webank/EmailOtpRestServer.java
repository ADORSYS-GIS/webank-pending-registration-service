package com.adorsys.webank;

import com.adorsys.webank.dto.EmailOtpRequest;
import com.adorsys.webank.dto.EmailOtpValidationRequest;
import com.adorsys.webank.dto.response.EmailResponse;
import com.adorsys.webank.dto.response.EmailValidationResponse;
import com.adorsys.webank.service.EmailOtpServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
public class EmailOtpRestServer implements EmailOtpRestApi {
    private static final Logger log = LoggerFactory.getLogger(EmailOtpRestServer.class);
    private final EmailOtpServiceApi emailOtpService;

    @Override
    public ResponseEntity<EmailResponse> sendEmailOtp(EmailOtpRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to send email OTP [correlationId={}]", correlationId);
        
        String maskedEmail = maskEmail(request.getEmail());
        log.debug("Processing email OTP request for email: {} [correlationId={}]", maskedEmail, correlationId);
        
        EmailResponse response = emailOtpService.sendEmailOtp(request.getAccountId(), request.getEmail());
        log.info("Email OTP request processed [correlationId={}]", correlationId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<EmailValidationResponse> validateEmailOtp(EmailOtpValidationRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to validate email OTP [correlationId={}]", correlationId);
        
        String maskedEmail = maskEmail(request.getEmail());
        log.debug("Validating OTP for email: {} [correlationId={}]", maskedEmail, correlationId);

        EmailValidationResponse response = emailOtpService.validateEmailOtp(
                request.getEmail(),
                request.getOtpInput(),
                request.getAccountId()
        );
        log.info("Email OTP validation processed [correlationId={}]", correlationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Masks an email address for logging purposes
     * Shows only first 2 and domain part
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return "********";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return email.charAt(0) + "****" + email.substring(atIndex);
        }
        
        return email.substring(0, 2) + "****" + email.substring(atIndex);
    }
}
