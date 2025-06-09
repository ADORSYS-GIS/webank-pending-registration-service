package com.adorsys.webank.serviceimpl;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.adorsys.webank.config.*;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.service.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;
import org.slf4j.MDC;

import java.util.*;

@Service
public class TokenServiceImpl implements TokenServiceApi {

    private static final Logger log = LoggerFactory.getLogger(TokenServiceImpl.class);

    @Autowired
    private KeyLoader keyLoader;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-time-ms}")
    private Long expirationTimeMs;

    @Override
    @Transactional
    public String requestRecoveryToken(TokenRequest tokenRequest) {
        String correlationId = MDC.get("correlationId");
        if (tokenRequest == null) {
            log.warn("Received null token request [correlationId={}]", correlationId);
            return null;
        }
        
        String oldAccountId = tokenRequest.getOldAccountId();
        String newAccountId = tokenRequest.getNewAccountId();
        
        log.info("Processing recovery token request for account migration [correlationId={}]", correlationId);
        log.debug("Migration from account: {} to account: {} [correlationId={}]", 
                maskAccountId(oldAccountId), maskAccountId(newAccountId), correlationId);
        
        try {
            String token = generateToken(oldAccountId, newAccountId);
            log.info("Recovery token generated successfully [correlationId={}]", correlationId);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate recovery token [correlationId={}]", correlationId, e);
            return null;
        }
    }

    private String generateToken(String oldAccountId, String newAccountId) {
        String correlationId = MDC.get("correlationId");
        try {
            log.debug("Generating token for account migration: {} -> {} [correlationId={}]", 
                    maskAccountId(oldAccountId), maskAccountId(newAccountId), correlationId);
            
            // Load keys using KeyLoader
            ECKey serverPrivateKey = keyLoader.loadPrivateKey();
            ECKey serverPublicKey = keyLoader.loadPublicKey();
            JWSSigner signer = new ECDSASigner(serverPrivateKey);
            JWSHeader header = JwtUtils.createJwtHeader(serverPublicKey);

            // Create JWT Claims
            long issuedAt = System.currentTimeMillis() / 1000; // Convert to seconds
            long expirationTime = issuedAt + (expirationTimeMs / 1000); // Convert milliseconds to seconds

            log.debug("Creating JWT claims with issuer: {}, expiration: {} seconds [correlationId={}]", 
                    issuer, expirationTimeMs/1000, correlationId);
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject("RecoveryToken")
                    .claim("oldAccountId", oldAccountId)
                    .claim("newAccountId", newAccountId)
                    .issueTime(new Date(issuedAt * 1000))
                    .expirationTime(new Date(expirationTime * 1000))
                    .build();

            // Sign the JWT
            log.debug("Signing JWT [correlationId={}]", correlationId);
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);

            String signedToken = signedJWT.serialize();
            log.info("Recovery token generated successfully with expiration in {} seconds [correlationId={}]", 
                    expirationTimeMs/1000, correlationId);
            
            if (log.isTraceEnabled()) {
                log.trace("Token: {} [correlationId={}]", signedToken, correlationId);
            }
            
            return signedToken;

        } catch (Exception e) {
            log.error("Error generating recovery token for accounts: {} -> {} [correlationId={}]", 
                    maskAccountId(oldAccountId), maskAccountId(newAccountId), correlationId, e);
            throw new IllegalStateException("Error generating recovery token", e);
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