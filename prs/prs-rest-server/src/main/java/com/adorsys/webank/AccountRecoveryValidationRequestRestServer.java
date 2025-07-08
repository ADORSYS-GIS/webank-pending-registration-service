package com.adorsys.webank;

import com.adorsys.webank.dto.AccountRecovery;
import com.adorsys.webank.dto.AccountRecoveryResponse;
import com.adorsys.webank.config.JwtValidator;
import com.adorsys.webank.service.AccountRecoveryValidationRequestServiceApi;
import com.nimbusds.jose.jwk.JWK;
import com.adorsys.error.JwtValidationException;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AccountRecoveryValidationRequestRestServer implements AccountRecoveryValidationRequestRestApi {

    private final AccountRecoveryValidationRequestServiceApi service;
    private static final Logger log = LoggerFactory.getLogger(AccountRecoveryValidationRequestRestServer.class);


    @Override
    public ResponseEntity<AccountRecoveryResponse> validateRecoveryToken(AccountRecovery accountRecovery) {
        String correlationId = MDC.get("correlationId");
        log.info("Received account recovery validation request [correlationId={}]", correlationId);

        try {
            log.debug(
                    "Validating JWT for account ID: {} [correlationId={}]",
                    maskAccountId(accountRecovery.getNewAccountId()),
                    correlationId
            );
        } catch (Exception e) {
            log.error(
                    "Error validating recovery token [correlationId={}]: {}",
                    correlationId,
                    e.getMessage()
            );
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }

        log.info("Processing account recovery [correlationId={}]", correlationId);
        AccountRecoveryResponse response = service.processRecovery(
                accountRecovery.getNewAccountId()
        );
        log.info("Account recovery validation completed [correlationId={}]", correlationId);
        return ResponseEntity.ok(response);
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
