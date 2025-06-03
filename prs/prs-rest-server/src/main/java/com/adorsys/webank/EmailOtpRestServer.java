package com.adorsys.webank;

import com.adorsys.webank.dto.EmailOtpRequest;
import com.adorsys.webank.dto.EmailOtpValidationRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.EmailOtpServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailOtpRestServer implements EmailOtpRestApi {
    private static final Logger log = LoggerFactory.getLogger(EmailOtpRestServer.class);
    private final EmailOtpServiceApi emailOtpService;
    private final CertValidator certValidator;

    public EmailOtpRestServer(EmailOtpServiceApi emailOtpService, CertValidator certValidator) {
        this.emailOtpService = emailOtpService;
        this.certValidator = certValidator;
    }

    @Override
    public String sendEmailOtp(String authorizationHeader, EmailOtpRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to send email OTP [correlationId={}]", correlationId);
        
        String jwtToken;
        try {
            log.debug("Extracting and validating JWT token [correlationId={}]", correlationId);
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String email = request.getEmail();
            String maskedEmail = maskEmail(email);
            
            log.debug("Validating JWT for email: {} [correlationId={}]", maskedEmail, correlationId);
            JwtValidator.validateAndExtract(jwtToken, email, request.getAccountId());

            log.debug("Validating certificate [correlationId={}]", correlationId);
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Invalid or unauthorized JWT [correlationId={}]", correlationId);
                return "Invalid or unauthorized JWT.";
            }
            
            log.debug("JWT validation successful [correlationId={}]", correlationId);
        } catch (Exception e) {
            log.error("JWT validation failed [correlationId={}]", correlationId, e);
            return "Invalid JWT: " + e.getMessage();
        }
        
        log.info("Sending email OTP [correlationId={}]", correlationId);
        String result = emailOtpService.sendEmailOtp(request.getAccountId(), request.getEmail());
        log.info("Email OTP request processed [correlationId={}]", correlationId);
        
        return result;
    }

    @Override
    public String validateEmailOtp(String authorizationHeader, EmailOtpValidationRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to validate email OTP [correlationId={}]", correlationId);
        
        String jwtToken;
        try {
            log.debug("Extracting and validating JWT token [correlationId={}]", correlationId);
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String email = request.getEmail();
            String maskedEmail = maskEmail(email);
            String otpInput = request.getOtp();
            String accountId = request.getAccountId();
            
            log.debug("Validating JWT for email: {} [correlationId={}]", maskedEmail, correlationId);
            JwtValidator.validateAndExtract(jwtToken, email, otpInput, accountId);

            log.debug("Validating certificate [correlationId={}]", correlationId);
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Invalid or unauthorized JWT [correlationId={}]", correlationId);
                return "Invalid or unauthorized JWT.";
            }
            
            log.debug("JWT validation successful [correlationId={}]", correlationId);
        } catch (Exception e) {
            log.error("JWT validation failed [correlationId={}]", correlationId, e);
            return "Invalid JWT: " + e.getMessage();
        }

        log.info("Validating email OTP [correlationId={}]", correlationId);
        String result = emailOtpService.validateEmailOtp(
                request.getEmail(),
                request.getOtp(),
                request.getAccountId()
        );
        
        log.info("Email OTP validation processed [correlationId={}]", correlationId);
        return result;
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Invalid authorization header format");
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
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
