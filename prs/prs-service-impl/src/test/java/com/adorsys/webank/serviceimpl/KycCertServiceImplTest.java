package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adorsys.error.ValidationException;
import com.adorsys.webank.config.CertGeneratorHelper;
import com.adorsys.webank.config.SecurityUtils;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;

@ExtendWith(MockitoExtension.class)
class KycCertServiceImplTest {

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @Mock
    private CertGeneratorHelper certGeneratorHelper;

    @InjectMocks
    private KycCertServiceImpl kycCertService;

    private ECKey deviceKey;
    private static final String TEST_ACCOUNT_ID = "test-account-id";

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException, JOSEException {
        deviceKey = new ECKeyGenerator(Curve.P_256).generate();
    }

    @Test
    void testGetCert_Success() throws Exception {
        // Arrange
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getStatus()).thenReturn(PersonalInfoStatus.APPROVED);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(personalInfo));
        when(certGeneratorHelper.generateCertificate(anyString())).thenReturn("test-certificate");

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);

            // Act
            String result = kycCertService.getCert(TEST_ACCOUNT_ID);

            // Assert
            assertNotNull(result);
            assertEquals("Your certificate is: test-certificate", result);
            verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
            verify(certGeneratorHelper).generateCertificate(anyString());
        }
    }

    @Test
    void testGetCert_NullAccountId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycCertService.getCert(null)
        );
        assertEquals("Account ID is required", exception.getMessage());
        verify(personalInfoRepository, never()).findByAccountId(anyString());
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testGetCert_EmptyAccountId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycCertService.getCert("")
        );
        assertEquals("Account ID is required", exception.getMessage());
        verify(personalInfoRepository, never()).findByAccountId(anyString());
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testGetCert_AccountNotFound() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // Act
        String result = kycCertService.getCert(TEST_ACCOUNT_ID);

        // Assert
        assertEquals(null, result);
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testGetCert_StatusPending() {
        // Arrange
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getStatus()).thenReturn(PersonalInfoStatus.PENDING);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(personalInfo));

        // Act
        String result = kycCertService.getCert(TEST_ACCOUNT_ID);

        // Assert
        assertEquals(null, result);
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testGetCert_StatusRejected() {
        // Arrange
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getStatus()).thenReturn(PersonalInfoStatus.REJECTED);
        when(personalInfo.getRejectionReason()).thenReturn("Invalid documents");

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(personalInfo));

        // Act
        String result = kycCertService.getCert(TEST_ACCOUNT_ID);

        // Assert
        assertEquals("REJECTED: Invalid documents", result);
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testGetCert_CertificateGenerationError() throws Exception {
        // Arrange
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getStatus()).thenReturn(PersonalInfoStatus.APPROVED);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(personalInfo));
        when(certGeneratorHelper.generateCertificate(anyString())).thenThrow(new RuntimeException("Certificate generation failed"));

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);

            // Act
            String result = kycCertService.getCert(TEST_ACCOUNT_ID);

            // Assert
            assertEquals("null", result);
            verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
            verify(certGeneratorHelper).generateCertificate(anyString());
        }
    }
}