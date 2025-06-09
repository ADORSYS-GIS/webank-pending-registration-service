package com.adorsys.webank.serviceimpl;
import  com.nimbusds.jose.crypto.ECDSASigner;
import com.adorsys.error.ValidationException;
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
        if (tokenRequest.getOldAccountId() == null || tokenRequest.getOldAccountId().isEmpty()) {
            throw new ValidationException("Old account ID is required");
        }
        if (tokenRequest.getNewAccountId() == null || tokenRequest.getNewAccountId().isEmpty()) {
            throw new ValidationException("New account ID is required");
        }
        try {
            return generateToken(tokenRequest.getOldAccountId(), tokenRequest.getNewAccountId());
        } catch (Exception e) {
            log.error("Error generating recovery token: ", e);
            return null;
        }
    }

    private String generateToken(String oldAccountId, String newAccountId) {
        try {
            // Load keys using KeyLoader
            ECKey serverPrivateKey = keyLoader.loadPrivateKey();
            ECKey serverPublicKey = keyLoader.loadPublicKey();
            JWSSigner signer = new ECDSASigner(serverPrivateKey);
            JWSHeader header = JwtUtils.createJwtHeader(serverPublicKey);

            // Create JWT Claims
            long issuedAt = System.currentTimeMillis() / 1000; // Convert to seconds
            long expirationTime = issuedAt + (expirationTimeMs / 1000); // Convert milliseconds to seconds

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject("RecoveryToken")
                    .claim("oldAccountId", oldAccountId)
                    .claim("newAccountId", newAccountId)
                    .issueTime(new Date(issuedAt * 1000))
                    .expirationTime(new Date(expirationTime * 1000))
                    .build();

            // Sign the JWT
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);

            String signedToken = signedJWT.serialize();
            log.info("Generated Recovery Token: " + signedToken);
            return signedToken;

        } catch (Exception e) {
            throw new IllegalStateException("Error generating recovery token", e);
        }
    }
}