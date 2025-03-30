/**package com.adorsys.webank.serviceimpl.security;

import com.adorsys.webank.security.CertGeneratorHelper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CertGeneratorHelperTest {

    private String serverPublicKeyJson;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException, JOSEException {
        // Generate an EC key pair for testing
        KeyPair keyPair = generateECKeyPair();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

        // Convert keys to JWK format using the correct Curve constant
        ECKey ecKey = new ECKey.Builder(Curve.P_256, publicKey)
                .privateKey(privateKey)
                .build();

        serverPublicKeyJson = ecKey.toPublicJWK().toJSONString();
    }

    @Test
    void testGenerateCertificate() throws Exception {
        // Arrange
        String deviceJwkJson = """
                {
                    "kty": "EC",
                    "crv": "P-256",
                    "x": "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
                    "y": "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM",
                    "kid": "device-key-id"
                }
                """;

        String issuer = "https://example.com";

        CertGeneratorHelper certGeneratorHelper = new CertGeneratorHelper();

        // Act
        String certificate = certGeneratorHelper.generateCertificate(deviceJwkJson);

        // Assert
        assertNotNull(certificate, "Generated certificate should not be null");

        SignedJWT signedJWT = SignedJWT.parse(certificate);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

        assertNotNull(claimsSet, "Claims set should not be null");
        assertEquals(issuer, claimsSet.getIssuer(), "Issuer claim should match");
        assertEquals(Collections.singletonList("device-key-id"), claimsSet.getAudience(), "Audience claim should match");
        assertTrue(claimsSet.getExpirationTime().after(new Date()), "Expiration time should be in the future");
        assertTrue(claimsSet.getIssueTime().before(new Date()), "Issue time should be in the past");

        // Verify the signature
        ECKey serverPublicKey = (ECKey) JWK.parse(serverPublicKeyJson);
        assertTrue(signedJWT.verify(new com.nimbusds.jose.crypto.ECDSAVerifier(serverPublicKey)), "Signature verification failed");
    }

    private KeyPair generateECKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256); // Use P-256 curve
        return keyPairGenerator.generateKeyPair();
    }
}**/