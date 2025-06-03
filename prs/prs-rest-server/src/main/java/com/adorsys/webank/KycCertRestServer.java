package com.adorsys.webank;

import com.adorsys.webank.exceptions.JwtValidationException;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.KycCertServiceApi;
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
public class KycCertRestServer implements KycCertRestApi {

    private final KycCertServiceApi kycCertServiceApi;
    private final CertValidator certValidator;

    public KycCertRestServer(KycCertServiceApi kycCertServiceApi, CertValidator certValidator) {
        this.kycCertServiceApi = kycCertServiceApi;
        this.certValidator = certValidator;
    }

    @Override
    public String getCert(String authorizationHeader, String accountId) {
        // Extract JWT token from header
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        log.debug("Processing KYC certificate request for accountId: {}", accountId);
        
        // Validate JWT and extract public key
        JWK publicKey = validateJwtAndExtractKey(jwtToken);
        
        // Retrieve and return the KYC certificate
        return kycCertServiceApi.getCert(publicKey, accountId);
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
