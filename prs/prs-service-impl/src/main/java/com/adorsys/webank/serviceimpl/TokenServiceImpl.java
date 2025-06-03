package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.TokenRequest;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.service.TokenServiceApi;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

@Service
public class TokenServiceImpl implements TokenServiceApi {

    private static final Logger log = LoggerFactory.getLogger(TokenServiceImpl.class);

    @Value("${server.private.key}")
    private String SERVER_PRIVATE_KEY_JSON;

    @Value("${server.public.key}")
    private String SERVER_PUBLIC_KEY_JSON;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-time-ms}")
    private Long expirationTimeMs;

    @Override
    @Transactional
    public String requestRecoveryToken(TokenRequest tokenRequest) {
        if (tokenRequest == null) {
            log.warn("Received null token request");
            return null;
        }
        
        String oldAccountId = tokenRequest.getOldAccountId();
        String newAccountId = tokenRequest.getNewAccountId();
        
        log.info("Processing recovery token request for account migration");
        log.debug("Migration from account: {} to account: {}", 
                maskAccountId(oldAccountId), maskAccountId(newAccountId));
        
        try {
            String token = generateToken(oldAccountId, newAccountId);
            log.info("Recovery token generated successfully");
            return token;
        } catch (Exception e) {
            log.error("Failed to generate recovery token", e);
            return null;
        }
    }

    private String generateToken(String oldAccountId, String newAccountId) {
        try {
            log.debug("Generating token for account migration: {} -> {}", 
                    maskAccountId(oldAccountId), maskAccountId(newAccountId));
            
            // Parse the server's private key
            log.debug("Parsing server private key");
            ECKey serverPrivateKey = (ECKey) JWK.parse(SERVER_PRIVATE_KEY_JSON);
            if (serverPrivateKey.getD() == null) {
                log.error("Private key 'd' parameter is missing");
                throw new IllegalStateException("Private key 'd' parameter is missing.");
            }

            // Create a signer with the server's private key
            log.debug("Creating JWT signer with server private key");
            JWSSigner signer = new ECDSASigner(serverPrivateKey);

            // Parse the server's public key
            log.debug("Parsing server public key");
            ECKey serverPublicKey = (ECKey) JWK.parse(SERVER_PUBLIC_KEY_JSON);

            // Compute SHA-256 hash of the server's public JWK to use as `kid`
            log.debug("Computing key ID (kid) from server public key");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(serverPublicKey.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8));
            String kid = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

            // Create JWT Header
            log.debug("Creating JWT header with kid: {}", kid);
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(kid) // Set 'kid' as SHA-256 of server public JWK
                    .type(JOSEObjectType.JWT)
                    .build();

            // Create JWT Claims
            long issuedAt = System.currentTimeMillis() / 1000; // Convert to seconds
            long expirationTime = issuedAt + (expirationTimeMs / 1000); // Convert milliseconds to seconds

            log.debug("Creating JWT claims with issuer: {}, expiration: {} seconds", 
                    issuer, expirationTimeMs/1000);
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject("RecoveryToken")
                    .claim("oldAccountId", oldAccountId)
                    .claim("newAccountId", newAccountId)
                    .issueTime(new Date(issuedAt * 1000))
                    .expirationTime(new Date(expirationTime * 1000))
                    .build();

            // Sign the JWT
            log.debug("Signing JWT");
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);

            String signedToken = signedJWT.serialize();
            log.info("Recovery token generated successfully with expiration in {} seconds", 
                    expirationTimeMs/1000);
            
            if (log.isTraceEnabled()) {
                log.trace("Token: {}", signedToken);
            }
            
            return signedToken;

        } catch (Exception e) {
            log.error("Error generating recovery token for accounts: {} -> {}", 
                    maskAccountId(oldAccountId), maskAccountId(newAccountId), e);
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