package com.adorsys.webank.config;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import  com.nimbusds.jose.jwk.ECKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertGeneratorHelper {

    private final KeyLoader keyLoader;

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
            String kid = computeKid(serverPublicKey);

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

        } catch (IllegalStateException | JOSEException | ParseException | NoSuchAlgorithmException e) {
            log.error("Error generating certificate", e);
            return "Error generating device certificate: " + e.getMessage();
        }
    }

    private String computeKid(ECKey serverPublicKey) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(serverPublicKey.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}