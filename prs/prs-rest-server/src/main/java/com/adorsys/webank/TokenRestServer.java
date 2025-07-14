package com.adorsys.webank;

import com.adorsys.webank.dto.TokenRequest;
import com.adorsys.webank.dto.response.KycRecoveryResponse;
import com.adorsys.webank.dto.response.TokenResponse;
import com.adorsys.webank.service.TokenServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TokenRestServer implements TokenRestApi {

    private static final Logger log = LoggerFactory.getLogger(TokenRestServer.class);
    private final TokenServiceApi tokenServiceApi;

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public ResponseEntity<TokenResponse> requestRecoveryToken(TokenRequest tokenRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received recovery token request [correlationId={}]", correlationId);

            TokenResponse result = tokenServiceApi.requestRecoveryToken(tokenRequest);
            log.debug("Processing recovery token request [correlationId={}]", correlationId);
            
            // Request the recovery token
            TokenResponse response = TokenResponse.builder()
                    .status(result.getStatus())
                    .message(result.getMessage())
                    .token(result.getToken())
                    .build();
            return ResponseEntity.ok(response);
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