package com.adorsys.webank;

import com.adorsys.webank.dto.AccountRecovery;
import com.adorsys.webank.dto.AccountRecoveryResponse;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.AccountRecoveryValidationRequestServiceApi;
import com.nimbusds.jose.jwk.JWK;
import com.adorsys.error.JwtValidationException;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        String jwtToken;
        JWK publicKey = null;
        String recoveryJwt = "";

        try {
            // Extract JWT from the header
            jwtToken = extractJwtFromHeader(authorizationHeader);

            // Validate JWT and extract the public key
            publicKey = JwtValidator.validateAndExtract(jwtToken, accountRecovery.getNewAccountId());

            // Extract the "RecoveryJWT" claim from the validated JWT
            recoveryJwt = JwtValidator.extractClaim(jwtToken, "recoveryJwt");
            log.info("Successfully extracted RecoveryJWT claim: {}", recoveryJwt);
            if (recoveryJwt == null || recoveryJwt.isEmpty()) {
                throw new BadRequestException("Invalid request. Missing recoveryJwt.");
            }

            log.info("Successfully extracted RecoveryJWT claim.");

        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }

        return ResponseEntity.ok(service.processRecovery(publicKey, accountRecovery.getNewAccountId(), recoveryJwt));
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new JwtValidationException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}
