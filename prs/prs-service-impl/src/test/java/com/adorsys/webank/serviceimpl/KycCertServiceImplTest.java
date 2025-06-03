package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.security.CertGeneratorHelper;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.adorsys.error.ValidationException;
import com.adorsys.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KycCertServiceImplTest {

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @Mock
    private CertGeneratorHelper certGeneratorHelper;

    @InjectMocks
    private KycCertServiceImpl kycCertService;

    private JWK publicKey;
    private static final String TEST_ACCOUNT_ID = "test-account-id";

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        MockitoAnnotations.openMocks(this);
        publicKey = generateTestPublicKey();
    }

    @Test
    void testGetCert_Success() throws Exception {
        // Arrange
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setAccountId(TEST_ACCOUNT_ID);
        personalInfo.setStatus(PersonalInfoStatus.APPROVED);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(personalInfo));
        when(certGeneratorHelper.generateCertificate(publicKey.toJSONString())).thenReturn("test-certificate");

        // Act
        String result = kycCertService.getCert(publicKey, TEST_ACCOUNT_ID);

        // Assert
        assertNotNull(result);
        assertEquals("Your certificate is: test-certificate", result);
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(certGeneratorHelper).generateCertificate(publicKey.toJSONString());
    }

    @Test
    void testGetCert_NullAccountId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycCertService.getCert(publicKey, null)
        );
        assertEquals("Account ID is required", exception.getMessage());
        verify(personalInfoRepository, never()).findByAccountId(anyString());
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testGetCert_EmptyAccountId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycCertService.getCert(publicKey, "")
        );
        assertEquals("Account ID is required", exception.getMessage());
        verify(personalInfoRepository, never()).findByAccountId(anyString());
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testGetCert_AccountNotFound() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            kycCertService.getCert(publicKey, TEST_ACCOUNT_ID)
        );
        assertEquals("No KYC certificate found for account ID: " + TEST_ACCOUNT_ID, exception.getMessage());
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testGetCert_StatusPending() {
        // Arrange
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setAccountId(TEST_ACCOUNT_ID);
        personalInfo.setStatus(PersonalInfoStatus.PENDING);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(personalInfo));

        // Act
        String result = kycCertService.getCert(publicKey, TEST_ACCOUNT_ID);

        // Assert
        assertEquals("null", result);
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testGetCert_StatusRejected() {
        // Arrange
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setAccountId(TEST_ACCOUNT_ID);
        personalInfo.setStatus(PersonalInfoStatus.REJECTED);
        personalInfo.setRejectionReason("Invalid documents");

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(personalInfo));

        // Act
        String result = kycCertService.getCert(publicKey, TEST_ACCOUNT_ID);

        // Assert
        assertEquals("REJECTED: Invalid documents", result);
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testGetCert_CertificateGenerationError() throws Exception {
        // Arrange
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setAccountId(TEST_ACCOUNT_ID);
        personalInfo.setStatus(PersonalInfoStatus.APPROVED);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(personalInfo));
        when(certGeneratorHelper.generateCertificate(publicKey.toJSONString())).thenThrow(new RuntimeException("Certificate generation failed"));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            kycCertService.getCert(publicKey, TEST_ACCOUNT_ID)
        );
        assertEquals("Error generating certificate", exception.getMessage());
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(certGeneratorHelper).generateCertificate(publicKey.toJSONString());
    }

    private JWK generateTestPublicKey() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateECKeyPair();
        return new ECKey.Builder(Curve.P_256, (java.security.interfaces.ECPublicKey) keyPair.getPublic()).build();
    }

    private KeyPair generateECKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256); // Use P-256 curve
        return keyPairGenerator.generateKeyPair();
    }
}