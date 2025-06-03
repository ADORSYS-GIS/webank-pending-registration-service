package com.adorsys.webank;

import com.adorsys.webank.dto.AccountRecovery;
import com.adorsys.webank.dto.AccountRecoveryResponse;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.AccountRecoveryValidationRequestServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountRecoveryValidationRequestRestServer implements AccountRecoveryValidationRequestRestApi {

    private final AccountRecoveryValidationRequestServiceApi service;
    private static final Logger log = LoggerFactory.getLogger(AccountRecoveryValidationRequestRestServer.class);

    public AccountRecoveryValidationRequestRestServer(AccountRecoveryValidationRequestServiceApi service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<AccountRecoveryResponse> validateRecoveryToken(String authorizationHeader, AccountRecovery accountRecovery) {
        String correlationId = MDC.get("correlationId");
        log.info("Received account recovery validation request [correlationId={}]", correlationId);
        
        String jwtToken;
        JWK publicKey = null;
        String recoveryJwt = "";

        try {
            // Extract JWT from the header
            log.debug("Extracting JWT from authorization header [correlationId={}]", correlationId);
            jwtToken = extractJwtFromHeader(authorizationHeader);

            // Validate JWT and extract the public key
            log.debug("Validating JWT for account ID: {} [correlationId={}]", 
                    maskAccountId(accountRecovery.getNewAccountId()), correlationId);
            publicKey = JwtValidator.validateAndExtract(jwtToken, accountRecovery.getNewAccountId());
            log.debug("JWT validation successful [correlationId={}]", correlationId);

            // Extract the "RecoveryJWT" claim from the validated JWT
            log.debug("Extracting RecoveryJWT claim [correlationId={}]", correlationId);
            recoveryJwt = JwtValidator.extractClaim(jwtToken, "recoveryJwt");
            
            if (recoveryJwt == null || recoveryJwt.isEmpty()) {
                log.warn("Missing recoveryJwt claim in JWT [correlationId={}]", correlationId);
                throw new BadRequestException("Invalid request. Missing recoveryJwt.");
            }

            log.debug("Successfully extracted RecoveryJWT claim [correlationId={}]", correlationId);

        } catch (Exception e) {
            log.error("Error validating recovery token [correlationId={}]: {}", 
                    correlationId, e.getMessage());
            return ResponseEntity.ok(null);
        }

        log.info("Processing account recovery [correlationId={}]", correlationId);
        AccountRecoveryResponse response = service.processRecovery(
                publicKey, 
                accountRecovery.getNewAccountId(), 
                recoveryJwt);
        
        log.info("Account recovery validation completed [correlationId={}]", correlationId);
        return ResponseEntity.ok(response);
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
     */
    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() < 5) {
            return "********";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
}
