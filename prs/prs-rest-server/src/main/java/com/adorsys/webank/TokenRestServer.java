package com.adorsys.webank;

import com.adorsys.webank.dto.TokenRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.TokenServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenRestServer implements TokenRestApi {

    private static final Logger log = LoggerFactory.getLogger(TokenRestServer.class);
    private final TokenServiceApi tokenServiceApi;
    private final CertValidator certValidator;

    public TokenRestServer(TokenServiceApi tokenServiceApi, CertValidator certValidator) {
        this.tokenServiceApi = tokenServiceApi;
        this.certValidator = certValidator;
    }

    @Override
    public String requestRecoveryToken(String authorizationHeader, TokenRequest tokenRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received recovery token request [correlationId={}]", correlationId);
        
        String oldAccountId = tokenRequest.getOldAccountId();
        String newAccountId = tokenRequest.getNewAccountId();
        
        // Add account IDs to MDC for logging
        MDC.put("oldAccountId", maskAccountId(oldAccountId));
        MDC.put("newAccountId", maskAccountId(newAccountId));
        
        String jwtToken;
        try {
            jwtToken = extractJwtFromHeader(authorizationHeader);
            log.debug("Validating JWT token for recovery token request [correlationId={}]", correlationId);
            
            JwtValidator.validateAndExtract(jwtToken, oldAccountId, newAccountId);

            // Validate the JWT token
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Unauthorized JWT token for recovery token request [correlationId={}]", correlationId);
                return "Unauthorized";
            }
            
            log.debug("JWT validation successful, generating recovery token [correlationId={}]", correlationId);
        } catch (Exception e) {
            log.error("JWT validation failed for recovery token request [correlationId={}]", 
                    correlationId, e);
            return "Invalid JWT: " + e.getMessage();
        }

        try {
            // Request the recovery token
            String token = tokenServiceApi.requestRecoveryToken(tokenRequest);
            log.info("Recovery token generated successfully [correlationId={}]", correlationId);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate recovery token [correlationId={}]", correlationId, e);
            return "Failed to generate recovery token: " + e.getMessage();
        } finally {
            // Clean up MDC
            MDC.remove("oldAccountId");
            MDC.remove("newAccountId");
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
     * Masks an account ID for logging purposes
     * Shows only first 2 and last 2 characters
     */
    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() < 5) {
            return "********";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
}