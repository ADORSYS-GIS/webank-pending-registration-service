package com.adorsys.webank.serviceimpl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class DeviceCertificateTest {

    private DeviceRegServiceImpl deviceRegService;
    private ECKey serverKeyPair;
    private ECKey deviceKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        // Generate test EC key pairs
        serverKeyPair = new ECKeyGenerator(Curve.P_256).keyID("server-key").generate();
        deviceKeyPair = new ECKeyGenerator(Curve.P_256).keyID("device-key").generate();

        // Initialize service and inject keys
        deviceRegService = new DeviceRegServiceImpl();
        injectField("SERVER_PRIVATE_KEY_JSON", serverKeyPair.toJSONString());
        injectField("SERVER_PUBLIC_KEY_JSON", serverKeyPair.toPublicJWK().toJSONString());
        injectField("expirationTimeMs", 60000L); // Set expiration time to 60 seconds for test
    }

    private void injectField(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = DeviceRegServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(deviceRegService, value);
    }

    @Test
    void generateDeviceCertificate_ValidJwtStructure() throws JOSEException, ParseException {
        // When
        String certificate = deviceRegService.generateDeviceCertificate(deviceKeyPair.toJSONString());
        SignedJWT signedJWT = SignedJWT.parse(certificate);

        // Verify header
        JWSHeader header = signedJWT.getHeader();
        assertEquals(JWSAlgorithm.ES256, header.getAlgorithm());
        assertEquals(JOSEObjectType.JWT, header.getType());
        assertNotNull(header.getKeyID(), "Key ID (kid) must be present in header");
    }

    @Test
    void generateDeviceCertificate_ValidClaims() throws ParseException {
        // When
        String certificate = deviceRegService.generateDeviceCertificate(deviceKeyPair.toJSONString());
        SignedJWT signedJWT = SignedJWT.parse(certificate);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        // Verify claims
        assertEquals("https://webank.com", claims.getIssuer());
        assertEquals("device-key", claims.getAudience().get(0));
        assertNotNull(claims.getClaim("cnf"), "Confirmation (cnf) claim must be present");
    }

    @Test
    void generateDeviceCertificate_ValidSignature() throws JOSEException, ParseException {
        // When
        String certificate = deviceRegService.generateDeviceCertificate(deviceKeyPair.toJSONString());
        SignedJWT signedJWT = SignedJWT.parse(certificate);

        // Verify signature
        JWSVerifier verifier = new ECDSAVerifier(serverKeyPair.toPublicJWK());
        assertTrue(signedJWT.verify(verifier), "Signature validation failed");
    }

    @Test
    void generateDeviceCertificate_ValidKeyIdComputation() throws NoSuchAlgorithmException, ParseException {
        // When
        String certificate = deviceRegService.generateDeviceCertificate(deviceKeyPair.toJSONString());
        SignedJWT signedJWT = SignedJWT.parse(certificate);

        // Compute expected 'kid'
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(serverKeyPair.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8));
        String expectedKid = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        assertEquals(expectedKid, signedJWT.getHeader().getKeyID(), "Incorrect 'kid' in JWT header");
    }
}
