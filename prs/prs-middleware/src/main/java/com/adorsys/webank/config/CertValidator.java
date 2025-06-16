package com.adorsys.webank.config;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.adorsys.webank.exceptions.SecurityConfigurationException;
import com.adorsys.webank.exceptions.JwtPayloadParseException;

import java.text.ParseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertValidator {

    private final KeyLoader keyLoader;

    public boolean validateJWT(String jwtToken) {
        try {
            SignedJWT signedJWT = parseJWT(jwtToken);
            String cert = extractCertificate(signedJWT);

            if (cert == null) {
                return false;
            }

            SignedJWT certJwt = parseJWT(cert);
            JWK jwk = keyLoader.loadPublicKey();
            return verifySignature(certJwt, jwk);
        } catch (ParseException e) {
            log.error("Error parsing JWT: ", e);
            throw new JwtPayloadParseException("Error parsing JWT", e);
        } catch (JOSEException e) {
            log.error("Error verifying JWT signature: ", e);
            throw new SecurityConfigurationException("Error verifying JWT signature", e);
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation: ", e);
            throw new SecurityConfigurationException("Unexpected error during JWT validation", e);
        }
    }

    private SignedJWT parseJWT(String token) throws ParseException {
        return SignedJWT.parse(token);
    }

    private String extractCertificate(SignedJWT signedJWT) {
        Object devJwt = signedJWT.getHeader().toJSONObject().get("devJwt");
        if (devJwt != null) {
            log.info("Extracted devJwt from header.");
            return devJwt.toString();
        }

        Object accountJwt = signedJWT.getHeader().toJSONObject().get("accountJwt");
        if (accountJwt != null) {
            log.info("Extracted accountJwt from header.");
            return accountJwt.toString();
        }

        log.error("No devJwt or accountJwt found in JWT header.");
        return null;
    }

    private boolean verifySignature(SignedJWT certJwt, JWK jwk) throws JOSEException {
        if (!(jwk instanceof ECKey publicKey) || jwk.isPrivate()) {
            throw new SecurityConfigurationException("Invalid JWK provided by backend", null);
        }

        JWSVerifier certVerifier = new ECDSAVerifier(publicKey);
        boolean isValid = certJwt.verify(certVerifier);

        if (!isValid) {
            log.error("Extracted JWT (devJwt/accountJwt) signature validation failed.");
        } else {
            log.info("JWT validation successful.");
        }

        return isValid;
    }
}