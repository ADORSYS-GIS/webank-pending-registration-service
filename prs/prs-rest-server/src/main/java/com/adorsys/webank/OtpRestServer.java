package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.OtpServiceApi;
import com.nimbusds.jose.jwk.JWK;
import com.adorsys.error.JwtValidationException;
import org.springframework.web.bind.annotation.RestController;

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
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String phoneNumber = request.getPhoneNumber();
            publicKey = JwtValidator.validateAndExtract(jwtToken, phoneNumber);

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }
        return otpService.sendOtp(publicKey, request.getPhoneNumber());
    }

    @Override
    public String validateOtp(String authorizationHeader, OtpValidationRequest request) {
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
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }

        return otpService.validateOtp(request.getPhoneNumber(), publicKey, request.getOtpInput());
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new JwtValidationException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}