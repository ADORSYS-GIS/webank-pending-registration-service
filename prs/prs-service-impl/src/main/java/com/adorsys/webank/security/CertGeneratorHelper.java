package com.adorsys.webank.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

public class CertGeneratorHelper {

    private final String serverPrivateKeyJson;
    private final String serverPublicKeyJson;
    private final String issuer;
    private final Long expirationTimeMs;

    public CertGeneratorHelper(String serverPrivateKeyJson, String serverPublicKeyJson, String issuer, Long expirationTimeMs) {
        this.serverPrivateKeyJson = serverPrivateKeyJson;
        this.serverPublicKeyJson = serverPublicKeyJson;
        this.issuer = issuer;
        this.expirationTimeMs = expirationTimeMs;
    }

    public String generateCertificate(String deviceJwkJson) {
        try {
            ECKey serverPrivateKey = (ECKey) JWK.parse(serverPrivateKeyJson);
            if (serverPrivateKey.getD() == null) {
                throw new IllegalStateException("Private key 'd' (private) parameter is missing.");
            }

            JWSSigner signer = new ECDSASigner(serverPrivateKey);
            ECKey serverPublicKey = (ECKey) JWK.parse(serverPublicKeyJson);
            String kid = computeKid(serverPublicKey);

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(kid)
                    .type(JOSEObjectType.JWT)
                    .build();

            JWK deviceJwk = JWK.parse(deviceJwkJson);
            long issuedAt = System.currentTimeMillis() / 1000;

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
            throw new IllegalStateException("Error generating device certificate", e);
        }
    }

    private String computeKid(ECKey serverPublicKey) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(serverPublicKey.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
