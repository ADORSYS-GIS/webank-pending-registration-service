package com.adorsys.webank;

import com.adorsys.webank.dto.TokenRequest;
import com.adorsys.webank.service.TokenServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public String requestRecoveryToken(String authorizationHeader, TokenRequest tokenRequest) {

        log.info("Requesting recovery token for old accountId: {}, new accountId: {}",
                tokenRequest.getOldAccountId(), tokenRequest.getNewAccountId());

        // Retrieve and return the recovery token
        return tokenServiceApi.requestRecoveryToken(tokenRequest);
    }
}