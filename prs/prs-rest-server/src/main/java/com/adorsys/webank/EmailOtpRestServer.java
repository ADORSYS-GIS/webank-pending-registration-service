package com.adorsys.webank;

import com.adorsys.webank.dto.EmailOtpRequest;
import com.adorsys.webank.dto.EmailOtpValidationRequest;
import com.adorsys.webank.service.EmailOtpServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class EmailOtpRestServer implements EmailOtpRestApi {
    private static final Logger log = LoggerFactory.getLogger(EmailOtpRestServer.class);
    private final EmailOtpServiceApi emailOtpService;

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendEmailOtp(String authorizationHeader, EmailOtpRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to send email OTP [correlationId={}]", correlationId);
        
        String maskedEmail = maskEmail(request.getEmail());
        log.debug("Processing email OTP request for email: {} [correlationId={}]", maskedEmail, correlationId);
        
        String result = emailOtpService.sendEmailOtp(request.getAccountId(), request.getEmail());
        log.info("Email OTP request processed [correlationId={}]", correlationId);
        
        return result;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String validateEmailOtp(String authorizationHeader, EmailOtpValidationRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to validate email OTP [correlationId={}]", correlationId);
        
        String maskedEmail = maskEmail(request.getEmail());
        log.debug("Validating OTP for email: {} [correlationId={}]", maskedEmail, correlationId);

        String result = emailOtpService.validateEmailOtp(
                request.getEmail(),
                request.getOtp(),
                request.getAccountId()
        );
        
        log.info("Email OTP validation processed [correlationId={}]", correlationId);
        return result;
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
