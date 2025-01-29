package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.OtpServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OtpRestServer implements OtpRestApi {
    private final OtpServiceApi otpService;

    public OtpRestServer(OtpServiceApi otpService) {

        this.otpService = otpService;
    }


    @Override
    public String sendOtp(String authorizationHeader, OtpRequest request) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String phoneNumber = request.getPhoneNumber();
            publicKey = JwtValidator.validateAndExtract(jwtToken, phoneNumber );


            // Validate the JWT token using CertValidator
            if (!CertValidator.validateJWT(jwtToken)) {
                return ("Invalid or unauthorized JWT.");
            }

            // Validate the JWT token
        } catch (Exception e) {
            return ("Invalid JWT: " + e.getMessage());
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
            publicKey = JwtValidator.validateAndExtract(jwtToken, phoneNumber );

            // Validate the JWT token using CertValidator
            if (!CertValidator.validateJWT(jwtToken)) {
                return ("Invalid or unauthorized JWT.");
            }

            // Validate the JWT token
        } catch (Exception e) {
            return ("Invalid JWT: " + e.getMessage());
        }

            return otpService.validateOtp(request.getPhoneNumber(),  publicKey ,request.getOtpInput(), request.getOtpHash() );
    }
    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}