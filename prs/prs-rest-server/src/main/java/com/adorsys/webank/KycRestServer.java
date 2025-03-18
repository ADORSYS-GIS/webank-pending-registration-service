package com.adorsys.webank;

import com.adorsys.webank.dto.*;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.KycServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KycRestServer implements KycRestApi {
    private final KycServiceApi kycServiceApi;
    private final CertValidator certValidator;  // Inject CertValidator as a dependency

    public KycRestServer(KycServiceApi kycServiceApi, CertValidator certValidator) {
        this.kycServiceApi = kycServiceApi;
        this.certValidator = certValidator;  // Assign the injected CertValidator instance
    }

    @Override
    public String sendKycDocument(String authorizationHeader, KycDocumentRequest kycDocumentRequest) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            publicKey = JwtValidator.validateAndExtract(jwtToken);

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {

                return "Invalid or unauthorized JWT.";
            }
        } catch (Exception e) {
            return "Invalid JWT: " + e.getMessage();
        }
        return kycServiceApi.sendKycDocument(publicKey, kycDocumentRequest);
    }

    @Override
    public String sendKycinfo(String authorizationHeader, KycInfoRequest kycInfoRequest) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            publicKey = JwtValidator.validateAndExtract(jwtToken);

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {

                return "Invalid or unauthorized JWT.";
            }
        } catch (Exception e) {
            return "Invalid JWT: " + e.getMessage();
        }

        return kycServiceApi.sendKycinfo(publicKey, kycInfoRequest);
    }

    @Override
    public String sendKyclocation(String authorizationHeader, KycLocationRequest kycLocationRequest) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            publicKey = JwtValidator.validateAndExtract(jwtToken);

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {

                return "Invalid or unauthorized JWT.";
            }
        } catch (Exception e) {
            return "Invalid JWT: " + e.getMessage();
        }

        return kycServiceApi.sendKyclocation(publicKey, kycLocationRequest);
    }

    @Override
    public String sendKycEmail(String authorizationHeader, KycEmailRequest kycEmailRequest) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            publicKey = JwtValidator.validateAndExtract(jwtToken);

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {

                return "Invalid or unauthorized JWT.";
            }
        } catch (Exception e) {
            return "Invalid JWT: " + e.getMessage();
        }

        return kycServiceApi.sendKycEmail(publicKey, kycEmailRequest);
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }


}