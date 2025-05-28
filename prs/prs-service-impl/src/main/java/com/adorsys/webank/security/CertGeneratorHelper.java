package com.adorsys.webank.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.jwk.ECKey;
import java.util.Collections;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertGeneratorHelper {

    private final KeyLoader keyLoader;
    private final HashHelper hashHelper;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-time-ms}")
    private Long expirationTimeMs;


    public String generateCertificate(String deviceJwkJson) {
        if (deviceJwkJson == null || deviceJwkJson.trim().isEmpty()) {
            log.error("Device JWK is null or empty");
            return "Error generating device certificate: Device JWK JSON must not be null or empty.";
        }

        try {
            ECKey serverPrivateKey = keyLoader.loadPrivateKey();
            ECKey serverPublicKey = keyLoader.loadPublicKey();
            String kid = hashHelper.computeKeyId(serverPublicKey);

            JWSSigner signer = new ECDSASigner(serverPrivateKey);
            JWK deviceJwk = JWK.parse(deviceJwkJson);
            long issuedAt = System.currentTimeMillis() / 1000;

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(kid)
                    .type(JOSEObjectType.JWT)
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(deviceJwk.getKeyID())
                    .claim("cnf", Collections.singletonMap("jwk", deviceJwk.toJSONObject()))
                    .issueTime(new Date(issuedAt * 1000))
                    .expirationTime(new Date((issuedAt + (expirationTimeMs / 1000)) * 1000))
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (Exception e) {
            log.error("Error generating certificate", e);
            return "Error generating device certificate: " + e.getMessage();
        }
    }
}