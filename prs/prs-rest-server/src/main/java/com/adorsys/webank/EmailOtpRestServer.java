package com.adorsys.webank;

import com.adorsys.webank.dto.EmailOtpRequest;
import com.adorsys.webank.dto.EmailOtpValidationRequest;
import com.adorsys.webank.exceptions.JwtValidationException;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.EmailOtpServiceApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.proc.BadJOSEException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

@RestController
@Slf4j
public class EmailOtpRestServer implements EmailOtpRestApi {
    private final EmailOtpServiceApi emailOtpService;
    private final CertValidator certValidator;

    public EmailOtpRestServer(EmailOtpServiceApi emailOtpService, CertValidator certValidator) {
        this.emailOtpService = emailOtpService;
        this.certValidator = certValidator;
    }

    @Override
    public String sendEmailOtp(String authorizationHeader, EmailOtpRequest request) {
        // Extract JWT token from header and validate it
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        validateJwtAndExtractKey(jwtToken, request.getEmail(), request.getAccountId());
        log.debug("JWT validation successful for email OTP request: {}", request.getEmail());
            
        // Call service to send email OTP
        return emailOtpService.sendEmailOtp(request.getAccountId(), request.getEmail());
    }
    
    /**
     * Validates JWT token and extracts the public key
     * @throws JwtValidationException if validation fails
     */
    private JWK validateJwtAndExtractKey(String jwtToken, String... params) {
        try {
            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("JWT signature validation failed");
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }
            
            // Only validate and extract the public key after JWT validation passes
            return JwtValidator.validateAndExtract(jwtToken, params);
        } catch (ParseException | JOSEException | BadJOSEException | NoSuchAlgorithmException | JsonProcessingException | JwtValidationException e) {
            // Handle all exceptions in a single catch block
            log.warn("JWT validation failed: {}", e.getMessage());
            
            if (e instanceof JwtValidationException) {
                throw (JwtValidationException) e;  // Cast and throw without creating new exception
            } else {
                throw new JwtValidationException("Invalid JWT: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String validateEmailOtp(String authorizationHeader, EmailOtpValidationRequest request) {
        // Extract JWT token from header and validate it
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        validateJwtAndExtractKey(jwtToken, request.getEmail(), request.getOtp(), request.getAccountId());
        log.debug("JWT validation successful for email OTP validation request: {}", request.getEmail());
        
        // Call service to validate email OTP
        return emailOtpService.validateEmailOtp(
                request.getEmail(),
                request.getOtp(),
                request.getAccountId()
        );
    }

    /**
     * Extracts JWT token from Authorization header
     * @throws JwtValidationException if header format is invalid
     */
    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new JwtValidationException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}
