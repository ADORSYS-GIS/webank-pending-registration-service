package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.OtpServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OtpRestServer implements OtpRestApi {
    private static final Logger log = LoggerFactory.getLogger(OtpRestServer.class);
    
    private final OtpServiceApi otpService;
    private final CertValidator certValidator;  // Inject CertValidator as a dependency

    public OtpRestServer(OtpServiceApi otpService, CertValidator certValidator) {
        this.otpService = otpService;
        this.certValidator = certValidator;  // Assign the injected CertValidator instance
    }

    @Override
    public String sendOtp(String authorizationHeader, OtpRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("Received OTP send request [correlationId={}]", correlationId);
        
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String phoneNumber = request.getPhoneNumber();
            
            log.debug("Validating JWT token and extracting public key [correlationId={}]", correlationId);
            publicKey = JwtValidator.validateAndExtract(jwtToken, phoneNumber);

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Invalid JWT token validation [correlationId={}]", correlationId);
                return "Invalid or unauthorized JWT.";
            }
            
            log.debug("JWT validation successful, sending OTP [correlationId={}]", correlationId);
        } catch (Exception e) {
            log.error("JWT validation failed [correlationId={}]", correlationId, e);
            return "Invalid JWT: " + e.getMessage();
        }
        
        // Add user phone number to MDC (masked)
        MDC.put("phoneNumber", maskPhoneNumber(request.getPhoneNumber()));
        
        try {
            String result = otpService.sendOtp(publicKey, request.getPhoneNumber());
            log.info("OTP send operation completed successfully [correlationId={}]", correlationId);
            return result;
        } finally {
            // Remove any additional MDC values we added
            MDC.remove("phoneNumber");
        }
    }

    @Override
    public String validateOtp(String authorizationHeader, OtpValidationRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("Received OTP validation request [correlationId={}]", correlationId);
        
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String phoneNumber = request.getPhoneNumber();
            String otpInput = request.getOtpInput();
            
            log.debug("Validating JWT token for OTP validation [correlationId={}]", correlationId);
            publicKey = JwtValidator.validateAndExtract(jwtToken, phoneNumber, otpInput);

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Invalid JWT token for OTP validation [correlationId={}]", correlationId);
                return "Invalid or unauthorized JWT.";
            }
            
            log.debug("JWT validation successful for OTP validation [correlationId={}]", correlationId);
        } catch (Exception e) {
            log.error("JWT validation failed for OTP validation [correlationId={}]", correlationId, e);
            return "Invalid JWT: " + e.getMessage();
        }

        // Add user phone number to MDC (masked)
        MDC.put("phoneNumber", maskPhoneNumber(request.getPhoneNumber()));
        
        try {
            String result = otpService.validateOtp(request.getPhoneNumber(), publicKey, request.getOtpInput());
            log.info("OTP validation operation completed [correlationId={}]", correlationId);
            return result;
        } finally {
            // Remove any additional MDC values we added
            MDC.remove("phoneNumber");
        }
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Invalid authorization header format");
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
    
    /**
     * Masks a phone number for logging purposes
     * Shows only last 4 digits, rest are masked
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "********";
        }
        return "******" + phoneNumber.substring(Math.max(0, phoneNumber.length() - 4));
    }
}