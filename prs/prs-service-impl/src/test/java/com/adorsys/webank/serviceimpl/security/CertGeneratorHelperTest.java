package com.adorsys.webank.serviceimpl.security;

import com.adorsys.webank.config.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.*;
import org.springframework.test.util.*;

import java.security.*;
import java.security.interfaces.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CertGeneratorHelperTest {

    private CertGeneratorHelper certGeneratorHelper;

    private ECKey deviceKey;

    @BeforeEach
    public void setUp() throws Exception {
    KeyLoader keyLoader = mock(KeyLoader.class);
        certGeneratorHelper = new CertGeneratorHelper(keyLoader);

        // Set issuer and expirationTimeMs using reflection
        ReflectionTestUtils.setField(certGeneratorHelper, "issuer", "test-issuer");
        ReflectionTestUtils.setField(certGeneratorHelper, "expirationTimeMs", 3600000L); // 1 hour

        // Generate EC key pair
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(Curve.P_256.toECParameterSpec());
        KeyPair keyPair = generator.generateKeyPair();

        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();

        ECKey ecPrivateJWK = new ECKey.Builder(Curve.P_256, publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        ECKey ecPublicJWK = ecPrivateJWK.toPublicJWK();

        when(keyLoader.loadPrivateKey()).thenReturn(ecPrivateJWK);
        when(keyLoader.loadPublicKey()).thenReturn(ecPublicJWK);

        // Build a mock device JWK (public key only)
        deviceKey = new ECKey.Builder(Curve.P_256, publicKey)
                .keyID("device-key-123")
                .build();
    }

    @Test
    public void testGenerateCertificateSuccess() throws Exception {
        String jwt = certGeneratorHelper.generateCertificate(deviceKey.toJSONString());

        assertNotNull(jwt, "JWT should not be null");

        JWSObject jwsObject = JWSObject.parse(jwt);
        assertEquals("test-issuer", jwsObject.getPayload().toJSONObject().get("iss"), "Issuer should match");

    }

    @Test
    public void testGenerateCertificateWithNullDeviceKey() {
        String result = certGeneratorHelper.generateCertificate(null);
        assertTrue(result.startsWith("Error generating device certificate"), "Should return error for null input");
    }

    @Test
    public void testGenerateCertificateWithEmptyDeviceKey() {
        String result = certGeneratorHelper.generateCertificate("   ");
        assertTrue(result.startsWith("Error generating device certificate"), "Should return error for empty input");
    }
}
