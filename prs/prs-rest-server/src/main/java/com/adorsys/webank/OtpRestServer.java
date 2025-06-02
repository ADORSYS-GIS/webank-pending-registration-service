package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.dto.response.OtpResponse;
import com.adorsys.webank.dto.response.OtpValidationResponse;
import com.adorsys.webank.exception.InvalidRequestException;
import com.adorsys.webank.exception.AuthenticationException;
import com.adorsys.webank.exception.ServiceException;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.OtpServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@RestController
public class OtpRestServer implements OtpRestApi {
    private static final Logger log = LoggerFactory.getLogger(OtpRestServer.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final Pattern OTP_PATTERN = Pattern.compile("^\\d{5}$");
    
    private final OtpServiceApi otpService;
    private final CertValidator certValidator;

    public OtpRestServer(OtpServiceApi otpService, CertValidator certValidator) {
        this.otpService = otpService;
        this.certValidator = certValidator;
    }

    @Override
    public ResponseEntity<OtpResponse> sendOtp(String authorizationHeader, OtpRequest request) {
        log.info("Processing OTP send request");
        
        // Validate request
        validateOtpRequest(request);
        
        // Authenticate request
        JWK publicKey = authenticateOtpRequest(authorizationHeader, request.getPhoneNumber());
        
        try {
            // Call the service and get the OTP hash
            log.info("Sending OTP to phone number: {}", request.getPhoneNumber());
            String otpHash = otpService.sendOtp(publicKey, request.getPhoneNumber());
            
            // Create and return the response
            OtpResponse response = createOtpResponse(otpHash, request.getPhoneNumber());
            
            log.info("OTP sent successfully to {}, expires at {}", request.getPhoneNumber(), response.getExpiresAt());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending OTP: {}", e.getMessage());
            throw new ServiceException("Failed to send OTP: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<OtpValidationResponse> validateOtp(String authorizationHeader, OtpValidationRequest request) {
        log.info("Processing OTP validation request");
        
        // Validate request
        validateOtpValidationRequest(request);
        
        // Authenticate request
        JWK publicKey = authenticateOtpValidationRequest(authorizationHeader, request);
        
        try {
            // Call the service and get the validation result
            log.info("Validating OTP for phone number: {}", request.getPhoneNumber());
            String validationResult = otpService.validateOtp(request.getPhoneNumber(), publicKey, request.getOtpInput());
            
            // Create response
            OtpValidationResponse response = createOtpValidationResponse(validationResult);
            
            log.info("OTP validation result for {}: {}", 
                    request.getPhoneNumber(), 
                    response.isValid() ? "SUCCESS" : "FAILED");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error validating OTP: {}", e.getMessage());
            throw new ServiceException("Failed to validate OTP: " + e.getMessage());
        }
    }
    
    private void validateOtpRequest(OtpRequest request) {
        if (request == null || request.getPhoneNumber() == null) {
            log.error("Missing phone number in OTP request");
            throw new InvalidRequestException("Phone number is required");
        }
        
        if (!PHONE_PATTERN.matcher(request.getPhoneNumber()).matches()) {
            log.error("Invalid phone number format: {}", request.getPhoneNumber());
            throw new InvalidRequestException("Invalid phone number format. Must match +[country code][number]");
        }
    }
    
    private void validateOtpValidationRequest(OtpValidationRequest request) {
        if (request == null) {
            log.error("Missing OTP validation request");
            throw new InvalidRequestException("Request body is required");
        }
        
        validatePhoneNumber(request.getPhoneNumber());
        validateOtpCode(request.getOtpInput());
    }
    
    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            log.error("Missing phone number in validation request");
            throw new InvalidRequestException("Phone number is required");
        }
        
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            log.error("Invalid phone number format: {}", phoneNumber);
            throw new InvalidRequestException("Invalid phone number format");
        }
    }
    
    private void validateOtpCode(String otpCode) {
        if (otpCode == null || otpCode.isEmpty()) {
            log.error("Missing OTP code in validation request");
            throw new InvalidRequestException("OTP code is required");
        }
        
        if (!OTP_PATTERN.matcher(otpCode).matches()) {
            log.error("Invalid OTP format: {}", otpCode);
            throw new InvalidRequestException("OTP must be 5 digits");
        }
    }
    
    private JWK authenticateOtpRequest(String authorizationHeader, String phoneNumber) {
        try {
            // Extract JWT and validate
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.debug("Validating JWT token for phone: {}", phoneNumber);
            
            // Validate certificate
            validateCertificate(jwtToken);
            
            // Extract and return public key
            return JwtValidator.validateAndExtract(jwtToken, phoneNumber);
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            throw new AuthenticationException("Authentication failed: " + e.getMessage());
        }
    }
    
    private JWK authenticateOtpValidationRequest(String authorizationHeader, OtpValidationRequest request) {
        try {
            // Extract JWT and validate
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.debug("Validating JWT for phone: {} and OTP: {}", request.getPhoneNumber(), request.getOtpInput());
            
            // Validate certificate
            validateCertificate(jwtToken);
            
            // Extract and return public key
            return JwtValidator.validateAndExtract(jwtToken, request.getPhoneNumber(), request.getOtpInput());
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            throw new AuthenticationException("Authentication failed: " + e.getMessage());
        }
    }
    
    private void validateCertificate(String jwtToken) {
        if (!certValidator.validateJWT(jwtToken)) {
            log.error("JWT certificate validation failed");
            throw new AuthenticationException("Invalid certificate");
        }
    }
    
    private OtpResponse createOtpResponse(String otpHash, String phoneNumber) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        OtpResponse response = new OtpResponse();
        response.setOtpHash(otpHash);
        response.setPhoneNumber(phoneNumber);
        response.setExpiresAt(expiresAt);
        response.setValiditySeconds(300);
        response.setSent(true);
        return response;
    }
    
    private OtpValidationResponse createOtpValidationResponse(String validationResult) {
        boolean isValid = validationResult != null && 
                         (validationResult.toLowerCase().contains("success") || 
                          validationResult.toLowerCase().contains("validated"));
        
        return new OtpValidationResponse(
            isValid,
            isValid ? "OTP validated successfully" : validationResult,
            null
        );
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AuthenticationException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}