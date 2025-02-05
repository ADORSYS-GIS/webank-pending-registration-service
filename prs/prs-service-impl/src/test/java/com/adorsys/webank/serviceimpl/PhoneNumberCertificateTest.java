package com.adorsys.webank.serviceimpl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.JSONObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class PhoneNumberCertificateTest {

    private OtpServiceImpl otpService;
    private ECKey serverKeyPair;
    private static final String phoneNumber = "+1234567890";
    private static final String devicePublicKey = "testPublicKey";

    @BeforeEach
    void setUp() throws Exception {
        // Generate test EC key pair
        serverKeyPair = new ECKeyGenerator(Curve.P_256)
                .keyID("123")
                .generate();

        // Initialize service and inject keys
        otpService = new OtpServiceImpl();
        injectField("SERVER_PRIVATE_KEY_JSON", serverKeyPair.toJSONString());
        injectField("SERVER_PUBLIC_KEY_JSON", serverKeyPair.toPublicJWK().toJSONString());
    }

    private void injectField(String fieldName, String value) throws NoSuchFieldException, IllegalAccessException {
        Field field = OtpServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(otpService, value);
    }

    @Test
    void generatePhoneNumberCertificate_ValidJwtStructure() throws JOSEException, ParseException {
        // When
        String certificate = otpService.generatePhoneNumberCertificate(phoneNumber, devicePublicKey);

        // Then
        JWSObject jwsObject = JWSObject.parse(certificate);

        // Verify header
        JWSHeader header = jwsObject.getHeader();
        assertEquals(JWSAlgorithm.ES256, header.getAlgorithm());
        assertEquals(JOSEObjectType.JWT, header.getType());
        assertNotNull(header.getJWK(), "JWK must be present in header");

        // Verify JWK matches server public key
        JWK headerJWK = header.getJWK();
        assertEquals(serverKeyPair.toPublicJWK(), headerJWK);
    }

    @Test
    void generatePhoneNumberCertificate_ValidPayloadHashes() throws ParseException, NoSuchAlgorithmException {
        // When
        String certificate = otpService.generatePhoneNumberCertificate(phoneNumber, devicePublicKey);
        JWSObject jwsObject = JWSObject.parse(certificate);

        // Verify payload
        String payload = jwsObject.getPayload().toString();
        String phoneHash = JSONObjectUtils.getString(JSONObjectUtils.parse(payload), "phoneHash");
        String devicePubKeyHash = JSONObjectUtils.getString(JSONObjectUtils.parse(payload), "devicePubKeyHash");

        // Compute expected hashes
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String expectedPhoneHash = Base64.getEncoder().encodeToString(
                digest.digest(phoneNumber.getBytes(StandardCharsets.UTF_8))
        );
        digest.reset();
        String expectedDeviceHash = Base64.getEncoder().encodeToString(
                digest.digest(devicePublicKey.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(expectedPhoneHash, phoneHash);
        assertEquals(expectedDeviceHash, devicePubKeyHash);
    }

    @Test
    void generatePhoneNumberCertificate_ValidSignature() throws JOSEException, ParseException {
        // When
        String certificate = otpService.generatePhoneNumberCertificate(phoneNumber, devicePublicKey);
        JWSObject jwsObject = JWSObject.parse(certificate);

        // Verify signature
        JWSVerifier verifier = new ECDSAVerifier(serverKeyPair.toPublicJWK());
        assertTrue(jwsObject.verify(verifier), "Signature validation failed");
    }
}
