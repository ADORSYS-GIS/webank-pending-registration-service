package com.adorsys.webank.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
public class CertValidator {
    private static final Logger logger = LoggerFactory.getLogger(CertValidator.class);

    @Value("${server.public.key.json}")
    private String SERVER_PUBLIC_KEY_JSON;

    /**
     * Validates the JWT by extracting the devJwt or accountJwt from its header and verifying signatures.
     *
     * @param jwtToken The JWT token string to validate.
     * @return True if valid, false otherwise.
     */
    public boolean validateJWT(String jwtToken) {
        try {
            SignedJWT signedJWT = parseJWT(jwtToken);
            String cert = extractCertificate(signedJWT);

            if (cert == null) {
                return false;
            }

            SignedJWT certJwt = parseJWT(cert);
            JWK jwk = loadPublicKey();
            return verifySignature(certJwt, jwk);
        } catch (ParseException e) {
            logger.error("Error parsing JWT: ", e);
        } catch (JOSEException e) {
            logger.error("Error verifying JWT signature: ", e);
        } catch (Exception e) {
            logger.error("Unexpected error during JWT validation: ", e);
        }
        return false;
    }

    /**
     * Parses a JWT token string into a SignedJWT object.
     *
     * @param token The JWT string.
     * @return Parsed SignedJWT object.
     * @throws ParseException if parsing fails.
     */
    private SignedJWT parseJWT(String token) throws ParseException {
        return SignedJWT.parse(token);
    }

    /**
     * Extracts either "devJwt" or "accountJwt" from the JWT header.
     *
     * @param signedJWT The parsed JWT.
     * @return The extracted certificate JWT string, or null if not found.
     */
    private String extractCertificate(SignedJWT signedJWT) {
        Object devJwt = signedJWT.getHeader().toJSONObject().get("devJwt");
        if (devJwt != null) {
            logger.info("Extracted devJwt from header.");
            return devJwt.toString();
        }

        Object accountJwt = signedJWT.getHeader().toJSONObject().get("accountJwt");
        if (accountJwt != null) {
            logger.info("Extracted accountJwt from header. {} ", accountJwt);
            return accountJwt.toString();
        }

        logger.error("No devJwt or accountJwt found in JWT header.");
        return null;
    }

    /**
     * Loads the public key from configuration and converts it to a JWK.
     *
     * @return The parsed JWK.
     * @throws ParseException if JWK parsing fails.
     */
    private JWK loadPublicKey() throws ParseException {
        if (SERVER_PUBLIC_KEY_JSON == null || SERVER_PUBLIC_KEY_JSON.trim().isEmpty()) {
            throw new IllegalArgumentException("Public key JSON is missing in configuration.");
        }

        JWK jwk = JWK.parse(SERVER_PUBLIC_KEY_JSON);
        logger.info("Loaded JWK from backend: {}", jwk);
        return jwk;
    }

    /**
     * Verifies the JWT signature using the given JWK.
     *
     * @param certJwt The signed JWT to verify.
     * @param jwk     The public key JWK.
     * @return True if the signature is valid, false otherwise.
     * @throws JOSEException if verification fails.
     */
    private boolean verifySignature(SignedJWT certJwt, JWK jwk) throws JOSEException {
        if (!(jwk instanceof ECKey publicKey) || jwk.isPrivate()) {
            throw new IllegalArgumentException("Invalid JWK provided by backend.");
        }

        JWSVerifier certVerifier = new ECDSAVerifier(publicKey);
        boolean isValid = certJwt.verify(certVerifier);

        if (!isValid) {
            logger.error("Extracted JWT (devJwt/accountJwt) signature validation failed.");
        } else {
            logger.info("JWT validation successful.");
        }

        return isValid;
    }
}
