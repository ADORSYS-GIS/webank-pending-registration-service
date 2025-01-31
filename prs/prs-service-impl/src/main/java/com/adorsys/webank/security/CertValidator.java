package com.adorsys.webank.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CertValidator {
    private static final Logger logger = LoggerFactory.getLogger(CertValidator.class);

    @Value("${server.public.key.json}")
    private String SERVER_PUBLIC_KEY_JSON; // Now accessible in non-static methods

    /**
     * Validates the JWT by extracting the devCert from its header and verifying signatures.
     *
     * @param jwtToken The JWT token string to validate.
     * @return True if valid, false otherwise.
     */
    public boolean validateJWT(String jwtToken) {
        try {
            // Parse the main JWT token
            SignedJWT signedJWT = SignedJWT.parse(jwtToken);
            logger.info("Parsed JWT: {}", signedJWT);

            // Extract "devCert" from the JWT header
            Object devCertObj = signedJWT.getHeader().toJSONObject().get("devJwt");
            if (devCertObj == null) {
                throw new IllegalArgumentException("Missing devCert in JWT header.");
            }
            String devCert = devCertObj.toString();
            logger.info("Extracted devCert: {}", devCert);

            // Parse the devCert (another JWT)
            SignedJWT devCertJwt = SignedJWT.parse(devCert);
            logger.info("Parsed devCert JWT: {}", devCertJwt);

            // Load public key from configuration
            JWK jwk = JWK.parse(SERVER_PUBLIC_KEY_JSON);
            logger.info("Loaded JWK from backend: {}", jwk);

            // Validate the devCert signature using the backend-provided JWK
            if (!(jwk instanceof ECKey publicKey) || jwk.isPrivate()) {
                throw new IllegalArgumentException("Invalid JWK provided by backend.");
            }

            JWSVerifier devCertVerifier = new ECDSAVerifier(publicKey);
            if (!devCertJwt.verify(devCertVerifier)) {
                logger.error("devCert signature validation failed.");
                return false;
            }

            // Validate the main JWT using the same public key
            JWSVerifier jwtVerifier = new ECDSAVerifier(publicKey);
            if (!signedJWT.verify(jwtVerifier)) {
                logger.error("JWT signature validation failed.");
                return false;
            }

            logger.info("JWT validation successful.");
            return true;
        } catch (Exception e) {
            logger.error("Error during JWT validation: ", e);
            return false;
        }
    }
}
