package com.adorsys.webank;

import com.adorsys.webank.dto.TokenRequest;
import com.adorsys.webank.service.TokenServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
    public String requestRecoveryToken(TokenRequest tokenRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received recovery token request [correlationId={}]", correlationId);
        
        String oldAccountId = tokenRequest.getOldAccountId();
        String newAccountId = tokenRequest.getNewAccountId();
        
        // Add account IDs to MDC for logging
        MDC.put("oldAccountId", maskAccountId(oldAccountId));
        MDC.put("newAccountId", maskAccountId(newAccountId));
        
        try {
            log.debug("Processing recovery token request [correlationId={}]", correlationId);
            
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