package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.dto.response.ErrorResponse;
import com.adorsys.webank.dto.response.OtpResponse;
import com.adorsys.webank.dto.response.ValidationResponse;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.OtpServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class OtpRestServer implements OtpRestApi {
    private final OtpServiceApi otpService;
    private final CertValidator certValidator;  // Inject CertValidator as a dependency

    public OtpRestServer(OtpServiceApi otpService, CertValidator certValidator) {
        this.otpService = otpService;
        this.certValidator = certValidator;  // Assign the injected CertValidator instance
    }

    @Override
    public ResponseEntity<OtpResponse> sendOtp(String authorizationHeader, OtpRequest request) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String phoneNumber = request.getPhoneNumber();
            publicKey = JwtValidator.validateAndExtract(jwtToken, phoneNumber);

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(null);
        }
        
        // Call the service and get the OTP hash
        String otpHash = otpService.sendOtp(publicKey, request.getPhoneNumber());
        
        // Create and return the response
        OtpResponse response = new OtpResponse();
        response.setOtpHash(otpHash);
        response.setPhoneNumber(request.getPhoneNumber());
        response.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        response.setValiditySeconds(300);
        response.setSent(true);
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ValidationResponse> validateOtp(String authorizationHeader, OtpValidationRequest request) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String phoneNumber = request.getPhoneNumber();
            String otpInput = request.getOtpInput();
            publicKey = JwtValidator.validateAndExtract(jwtToken, phoneNumber, otpInput);

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ValidationResponse(false, "Invalid or unauthorized JWT", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ValidationResponse(false, "Invalid JWT: " + e.getMessage(), null));
        }

        // Call the service and get the validation result
        String validationResult = otpService.validateOtp(request.getPhoneNumber(), publicKey, request.getOtpInput());
        
        // Parse the result and create response
        boolean isValid = validationResult != null && validationResult.contains("success");
        ValidationResponse response = new ValidationResponse(
            isValid,
            isValid ? "OTP validated successfully" : validationResult,
            null
        );
        
        return ResponseEntity.ok(response);
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}