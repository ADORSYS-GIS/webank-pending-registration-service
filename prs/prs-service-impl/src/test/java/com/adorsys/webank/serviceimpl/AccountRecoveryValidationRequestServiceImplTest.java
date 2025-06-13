package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.AccountRecoveryResponse;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.adorsys.webank.config.CertGeneratorHelper;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

        // Generate a test public key
        publicKey = generateTestPublicKey();

        // Generate a valid recovery JWT
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
        String oldAccountId = "oldAccountId";
        assertEquals(oldAccountId, response.getAccountId(), "Account ID should match");
        assertEquals(newKycCertificate, response.getKycCertificate(), "KYC certificate should match");
        assertEquals("Account recovery successful", response.getMessage(), "Message should indicate success");

        verify(certGeneratorHelper, times(1)).generateCertificate(anyString());
    }

    @Test
    void testProcessRecovery_InvalidRecoveryJwtFormat() {
        // Arrange
        String invalidRecoveryJwt = "invalid.jwt.format";

        // Act
        AccountRecoveryResponse response = accountRecoveryService.processRecovery(publicKey, newAccountId, invalidRecoveryJwt);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertNull(response.getAccountId(), "Account ID should be null");
        assertNull(response.getKycCertificate(), "KYC certificate should be null");
        assertEquals("Invalid RecoveryJWT format", response.getMessage(), "Message should indicate invalid JWT format");

        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testProcessRecovery_ClaimingAccountIdMismatch() throws Exception {
        // Arrange
        String mismatchedRecoveryJwt = generateMismatchedRecoveryJwt(newAccountId);

        // Act
        AccountRecoveryResponse response = accountRecoveryService.processRecovery(publicKey, newAccountId, mismatchedRecoveryJwt);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertNull(response.getAccountId(), "Account ID should be null");
        assertNull(response.getKycCertificate(), "KYC certificate should be null");
        assertEquals("Claiming account ID mismatch", response.getMessage(), "Message should indicate account ID mismatch");

        verify(certGeneratorHelper, never()).generateCertificate(anyString());
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


    private String generateMismatchedRecoveryJwt(String newAccountId) throws JOSEException {
        this.newAccountId = newAccountId;
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