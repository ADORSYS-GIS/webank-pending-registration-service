package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.exceptions.JwtValidationException;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.OtpServiceApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.proc.BadJOSEException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

@Slf4j
@RestController
public class OtpRestServer implements OtpRestApi {
    private final OtpServiceApi otpService;
    private final CertValidator certValidator;  // Inject CertValidator as a dependency

    public OtpRestServer(OtpServiceApi otpService, CertValidator certValidator) {
        this.otpService = otpService;
        this.certValidator = certValidator;  // Assign the injected CertValidator instance
    }

    @Override
    public String sendOtp(String authorizationHeader, OtpRequest request) {
        // Extract and validate the JWT token
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        String phoneNumber = request.getPhoneNumber();
        
        // Validate JWT and extract public key
        JWK publicKey = validateJwtAndExtractKey(jwtToken, phoneNumber);
        
        // Send OTP using the validated data
        log.debug("Sending OTP for phone number: {}", phoneNumber);
        return otpService.sendOtp(publicKey, phoneNumber);
    }
    
    /**
     * Validates JWT token and extracts the public key
     * @throws JwtValidationException if validation fails
     */
    private JWK validateJwtAndExtractKey(String jwtToken, String phoneNumber) {
        try {
            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("JWT signature validation failed");
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }
            
            // Only validate and extract the public key after JWT validation passes
            return JwtValidator.validateAndExtract(jwtToken, phoneNumber);
        } catch (ParseException | JOSEException | BadJOSEException | NoSuchAlgorithmException | JsonProcessingException | JwtValidationException e) {
            // Handle all exceptions - no separate catch for JwtValidationException
            log.warn("JWT validation failed: {}", e.getMessage());
            
            if (e instanceof JwtValidationException) {
                throw (JwtValidationException) e;  // Cast and throw without creating new exception
            } else {
                throw new JwtValidationException("Invalid JWT: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String validateOtp(String authorizationHeader, OtpValidationRequest request) {
        // Extract and validate the JWT token
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        String phoneNumber = request.getPhoneNumber();
        String otpInput = request.getOtpInput();
        
        // Validate JWT and extract public key with OTP context
        JWK publicKey = validateJwtAndExtractKeyWithOtp(jwtToken, phoneNumber, otpInput);
        
        // Validate OTP using the service
        log.debug("Validating OTP for phone number: {}", phoneNumber);
        return otpService.validateOtp(phoneNumber, publicKey, otpInput);
    }
    
    /**
     * Validates JWT token with OTP context and extracts the public key
     * @throws JwtValidationException if validation fails
     */
    private JWK validateJwtAndExtractKeyWithOtp(String jwtToken, String phoneNumber, String otpInput) {
        try {
            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("JWT signature validation failed during OTP validation");
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }
            
            // Only validate and extract the public key after JWT validation passes
            return JwtValidator.validateAndExtract(jwtToken, phoneNumber, otpInput);
        } catch (ParseException | JOSEException | BadJOSEException | NoSuchAlgorithmException | JsonProcessingException | JwtValidationException e) {
            // Handle all exceptions in a single catch block
            if (e instanceof JwtValidationException) {
                log.debug("Passing through JWT validation exception: {}", e.getMessage());
                throw (JwtValidationException) e;  // Cast and throw without creating new exception
            } else {
                log.warn("JWT validation failed during OTP validation: {}", e.getMessage());
                throw new JwtValidationException("Invalid JWT during OTP validation: " + e.getMessage(), e);
            }
        }
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