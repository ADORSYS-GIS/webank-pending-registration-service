package com.adorsys.webank.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertValidator {
    private static final Logger logger = LoggerFactory.getLogger(CertValidator.class);

    /**
     * Validates the JWT by extracting the devCert from its header and verifying signatures.
     *
     * @param jwtToken The JWT token string to validate.
     * @return True if valid, false otherwise.
     */
    public static boolean validateJWT(String jwtToken) {
        try {
            // Parse the main JWT token
            SignedJWT signedJWT = SignedJWT.parse(jwtToken);
            logger.info(String.valueOf(signedJWT));

            // Extract "devCert" from the JWT header
            Object devCertObj = signedJWT.getHeader().toJSONObject().get("devJwt");
            if (devCertObj == null) {
                throw new IllegalArgumentException("Missing devCert in JWT header.");
            }
            String devCert = devCertObj.toString();
            logger.info(devCert);


            // Parse the devCert (another JWT)
            SignedJWT devCertJwt = SignedJWT.parse(devCert);

            logger.info(String.valueOf(devCertJwt));

            // Extract JWK from the devCert header
            JWK jwk = JWK.parse(devCertJwt.getHeader().getJWK().toJSONObject());
            logger.info(String.valueOf(jwk));



            // Validate the devCert signature using the JWK
            if (!(jwk instanceof ECKey) || jwk.isPrivate()) {
                throw new IllegalArgumentException("Invalid JWK in devCert.");
            }
            ECKey publicKey = (ECKey) jwk;

            JWSVerifier devCertVerifier = new ECDSAVerifier(publicKey);
            if (!devCertJwt.verify(devCertVerifier)) {
                logger.error("devCert signature validation failed.");
                return false;
            }

            // Validate the main JWT using the public key from devCert
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