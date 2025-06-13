package com.adorsys.webank.serviceimpl.security;

import com.adorsys.webank.config.CertGeneratorHelper;
import com.adorsys.webank.config.KeyLoader;
import com.adorsys.webank.properties.JwtProperties;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CertGeneratorHelperTest {

    private CertGeneratorHelper certGeneratorHelper;

    private ECKey deviceKey;

    @BeforeEach
    public void setUp() throws Exception {
        KeyLoader keyLoader = mock(KeyLoader.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        certGeneratorHelper = new CertGeneratorHelper(keyLoader, jwtProperties);

        when(jwtProperties.getIssuer()).thenReturn("test-issuer");
        when(jwtProperties.getExpirationTimeMs()).thenReturn(3600000L);

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
        assertThrows(IllegalArgumentException.class, () -> {
            certGeneratorHelper.generateCertificate(null);
        });
    }

    @Test
    public void testGenerateCertificateWithEmptyDeviceKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            certGeneratorHelper.generateCertificate("   ");
        });
    }
}
