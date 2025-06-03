package com.adorsys.webank;

import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.KycCertServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KycCertRestServer implements KycCertRestApi {

    private static final Logger log = LoggerFactory.getLogger(KycCertRestServer.class);
    private final KycCertServiceApi kycCertServiceApi;
    private final CertValidator certValidator;

    public KycCertRestServer(KycCertServiceApi kycCertServiceApi, CertValidator certValidator) {
        this.kycCertServiceApi = kycCertServiceApi;
        this.certValidator = certValidator;
    }

    @Override
    public String getCert(String authorizationHeader, String accountId) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to get KYC certificate for account: {} [correlationId={}]", 
                maskAccountId(accountId), correlationId);
        
        String jwtToken;
        JWK publicKey;
        try {
            log.debug("Extracting JWT from authorization header [correlationId={}]", correlationId);
            jwtToken = extractJwtFromHeader(authorizationHeader);
            
            log.debug("Validating JWT token [correlationId={}]", correlationId);
            publicKey = JwtValidator.validateAndExtract(jwtToken);
            log.debug("JWT validation successful, public key extracted [correlationId={}]", correlationId);

            // Validate the JWT token
            log.debug("Validating certificate [correlationId={}]", correlationId);
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Unauthorized access attempt - invalid JWT [correlationId={}]", correlationId);
                return "Unauthorized";
            }
            
            log.debug("Certificate validation successful [correlationId={}]", correlationId);
        } catch (Exception e) {
            log.error("Error validating JWT [correlationId={}]: {}", correlationId, e.getMessage());
            return "Invalid JWT: " + e.getMessage();
        }

        // Retrieve and return the KYC certificate
        log.info("Retrieving KYC certificate for account: {} [correlationId={}]", 
                maskAccountId(accountId), correlationId);
        String result = kycCertServiceApi.getCert(publicKey, accountId);
        log.info("KYC certificate retrieval completed [correlationId={}]", correlationId);
        
        return result;
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
