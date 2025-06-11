package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adorsys.error.ValidationException;
import com.adorsys.webank.config.CertGeneratorHelper;
import com.adorsys.webank.dto.AccountRecoveryResponse;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

class AccountRecoveryValidationRequestServiceImplTest {

    @Mock
    private CertGeneratorHelper certGeneratorHelper;

    @InjectMocks
    private AccountRecoveryValidationRequestServiceImpl accountRecoveryService;

    private JWK publicKey;
    private String newAccountId = "newAccountId";
    private String recoveryJwt;

    // Define a valid secret key for HS256 (32 bytes)
    private static final String SECRET_KEY = "a-very-long-secret-key-that-is-32-bytes";

    @BeforeEach
    void setUp() throws JOSEException {
        MockitoAnnotations.openMocks(this);
        publicKey = generateTestPublicKey();
        recoveryJwt = generateValidRecoveryJwt();
    }

    @Test
    void testProcessRecovery_SuccessfulRecovery() throws Exception {
        // Arrange
        String newKycCertificate = "newKycCertificate";
        when(certGeneratorHelper.generateCertificate(anyString())).thenReturn(newKycCertificate);

        // Act
        AccountRecoveryResponse response = accountRecoveryService.processRecovery(publicKey, newAccountId, recoveryJwt);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals("oldAccountId", response.getOldAccountId(), "Old account ID should match");
        assertEquals(newKycCertificate, response.getNewKycCertificate(), "New KYC certificate should match");
        assertEquals("Account recovery successful", response.getMessage(), "Message should indicate success");

        verify(certGeneratorHelper, times(1)).generateCertificate(anyString());
    }

    @Test
    void testProcessRecovery_NullNewAccountId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            accountRecoveryService.processRecovery(publicKey, null, recoveryJwt)
        );
        assertEquals("New account ID is required", exception.getMessage());
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testProcessRecovery_EmptyNewAccountId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            accountRecoveryService.processRecovery(publicKey, "", recoveryJwt)
        );
        assertEquals("New account ID is required", exception.getMessage());
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testProcessRecovery_NullRecoveryJwt() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            accountRecoveryService.processRecovery(publicKey, newAccountId, null)
        );
        assertEquals("Recovery JWT is required", exception.getMessage());
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testProcessRecovery_EmptyRecoveryJwt() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            accountRecoveryService.processRecovery(publicKey, newAccountId, "")
        );
        assertEquals("Recovery JWT is required", exception.getMessage());
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testProcessRecovery_InvalidRecoveryJwtFormat() {
        // Arrange
        String invalidRecoveryJwt = "invalid.jwt.format";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            accountRecoveryService.processRecovery(publicKey, newAccountId, invalidRecoveryJwt)
        );
        assertEquals("Invalid RecoveryJWT format", exception.getMessage());
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testProcessRecovery_ClaimingAccountIdMismatch() throws Exception {
        // Arrange
        String mismatchedRecoveryJwt = generateMismatchedRecoveryJwt();

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            accountRecoveryService.processRecovery(publicKey, newAccountId, mismatchedRecoveryJwt)
        );
        assertEquals("Claiming account ID mismatch", exception.getMessage());
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testProcessRecovery_CertificateGenerationError() throws Exception {
        // Arrange
        when(certGeneratorHelper.generateCertificate(anyString())).thenThrow(new RuntimeException("Certificate generation failed"));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            accountRecoveryService.processRecovery(publicKey, newAccountId, recoveryJwt)
        );
        assertTrue(exception.getMessage().contains("An unexpected error occurred"));
        verify(certGeneratorHelper, times(1)).generateCertificate(anyString());
    }

    private JWK generateTestPublicKey() throws JOSEException {
        return new com.nimbusds.jose.jwk.ECKey.Builder(com.nimbusds.jose.jwk.Curve.P_256,
                (java.security.interfaces.ECPublicKey) Objects.requireNonNull(generateECKeyPair()).getPublic()).build();
    }

    private String generateValidRecoveryJwt() throws JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("TimeStamp", System.currentTimeMillis())
                .claim("newAccountId", "newAccountId")
                .claim("oldAccountId", "oldAccountId")
                .build();

        SignedJWT signedJWT = new SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(new com.nimbusds.jose.crypto.MACSigner(SECRET_KEY.getBytes()));
        return signedJWT.serialize();
    }

    private String generateMismatchedRecoveryJwt() throws JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("TimeStamp", System.currentTimeMillis())
                .claim("newAccountId", "wrongAccountId")
                .claim("oldAccountId", "oldAccountId")
                .build();

        SignedJWT signedJWT = new SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(new com.nimbusds.jose.crypto.MACSigner(SECRET_KEY.getBytes()));
        return signedJWT.serialize();
    }

    private KeyPair generateECKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(256); // Use P-256 curve
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            return null;
        }
    }
}