package com.adorsys.webank;

import com.adorsys.webank.dto.TokenRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.TokenServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenRestServer implements TokenRestApi {

    private static final Logger log = LoggerFactory.getLogger(TokenRestServer.class);
    private final TokenServiceApi tokenServiceApi;
    private final CertValidator certValidator;

    public TokenRestServer( TokenServiceApi tokenServiceApi, CertValidator certValidator) {
        this.tokenServiceApi = tokenServiceApi;
        this.certValidator = certValidator;
    }

    @Override
    public String requestRecoveryToken(String authorizationHeader, TokenRequest tokenRequest) {
        String jwtToken;
        try {
            jwtToken = extractJwtFromHeader(authorizationHeader);
            JwtValidator.validateAndExtract(jwtToken);

            // Validate the JWT token
            if (!certValidator.validateJWT(jwtToken)) {
                return "Unauthorized";
            }
        } catch (Exception e) {
            return "Invalid JWT: " + e.getMessage();
        }

        // Retrieve and return the KYC certificate
        return tokenServiceApi.requestRecoveryToken( tokenRequest);
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }

}
