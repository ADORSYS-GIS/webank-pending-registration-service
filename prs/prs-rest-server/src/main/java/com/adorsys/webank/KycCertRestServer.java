package com.adorsys.webank;

import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.KycCertServiceApi;
import com.adorsys.webank.service.KycServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KycCertRestServer implements KycCertRestApi {

    private static final Logger log = LoggerFactory.getLogger(KycCertRestServer.class);
    private final KycCertServiceApi kycCertServiceApi;
    private final CertValidator certValidator;

    public KycCertRestServer( KycCertServiceApi kycCertServiceApi, CertValidator certValidator) {
        this.kycCertServiceApi = kycCertServiceApi;
        this.certValidator = certValidator;
    }

    @Override
    public String getCert(String authorizationHeader) {
        String jwtToken;
        JWK publicKey;
        try {
            jwtToken = extractJwtFromHeader(authorizationHeader);
            publicKey = JwtValidator.validateAndExtract(jwtToken);

            // Validate the JWT token
            if (!certValidator.validateJWT(jwtToken)) {
                return "Unauthorized";
            }
        } catch (Exception e) {
            return "Invalid JWT: " + e.getMessage();
        }

        // Retrieve and return the KYC certificate
        return kycCertServiceApi.getCert(publicKey);
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}
