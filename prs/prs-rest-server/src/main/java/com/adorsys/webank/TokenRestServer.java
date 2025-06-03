package com.adorsys.webank;

import com.adorsys.webank.dto.TokenRequest;
import com.adorsys.webank.exceptions.JwtValidationException;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.TokenServiceApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

@Slf4j
@RestController
public class TokenRestServer implements TokenRestApi {
    private final TokenServiceApi tokenServiceApi;
    private final CertValidator certValidator;

    public TokenRestServer( TokenServiceApi tokenServiceApi, CertValidator certValidator) {
        this.tokenServiceApi = tokenServiceApi;
        this.certValidator = certValidator;
    }

    @Override
    public String requestRecoveryToken(String authorizationHeader, TokenRequest tokenRequest) {
        log.debug("Processing recovery token request for oldAccountId: {}, newAccountId: {}", 
                tokenRequest.getOldAccountId(), tokenRequest.getNewAccountId());
                
        // Extract and validate JWT token
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        validateJwtForTokenRequest(jwtToken, tokenRequest);
        
        // Retrieve and return the recovery token
        log.info("JWT validated successfully, requesting recovery token");
        return tokenServiceApi.requestRecoveryToken(tokenRequest);
    }
    
    /**
     * Validates JWT token for token request
     * @throws JwtValidationException if validation fails
     */
    private void validateJwtForTokenRequest(String jwtToken, TokenRequest tokenRequest) {
        try {
            // Validate JWT payload matches request parameters
            JwtValidator.validateAndExtract(jwtToken, tokenRequest.getOldAccountId(), tokenRequest.getNewAccountId());
            
            // Validate JWT signature
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("JWT signature validation failed");
                throw new JwtValidationException("Unauthorized - Invalid JWT signature");
            }
            
            log.debug("JWT validation successful");
        } catch (JwtValidationException e) {
            // Simply pass through existing JwtValidationException
            log.debug("JWT validation exception occurred: {}", e.getMessage());
            throw e;
        } catch (ParseException | JOSEException | BadJOSEException | NoSuchAlgorithmException | JsonProcessingException e) {
            // Handle specific exceptions from JwtValidator
            log.warn("JWT validation failed: {}", e.getMessage());
            throw new JwtValidationException("Invalid JWT: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts JWT token from Authorization header
     * @throws JwtValidationException if header format is invalid
     */
    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Invalid authorization header format");
            throw new JwtValidationException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }

}