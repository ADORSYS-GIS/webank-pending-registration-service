package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.adorsys.error.ResourceNotFoundException;
import com.adorsys.error.ValidationException;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KycRecoveryServiceImplTest {

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @InjectMocks
    private KycRecoveryServiceImpl kycRecoveryService;

    private static final String TEST_ACCOUNT_ID = "test-account-id";
    private static final String TEST_ID_NUMBER = "test-id-number";
    private static final String TEST_EXPIRY_DATE = "2025-12-31";

    @BeforeEach
    void setUp() {
        // No additional setup needed
    }

    @Test
    void testVerifyKycRecoveryFields_Success() {
        // Arrange
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(personalInfo.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // Act
        String result = kycRecoveryService.verifyKycRecoveryFields(
                TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE);

        // Assert
        assertEquals("Document verification successful", result);
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void testVerifyKycRecoveryFields_NullAccountId() {
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(null, TEST_ID_NUMBER, TEST_EXPIRY_DATE)
        );
        assertEquals("No record found for accountId null", exception.getMessage());
        verify(personalInfoRepository).findByAccountId(null);
    }

    @Test
    void testVerifyKycRecoveryFields_EmptyAccountId() {
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields("", TEST_ID_NUMBER, TEST_EXPIRY_DATE)
        );
        assertEquals("No record found for accountId ", exception.getMessage());
        verify(personalInfoRepository).findByAccountId("");
    }

    @Test
    void testVerifyKycRecoveryFields_NullIdNumber() {
        // Arrange
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(personalInfo.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, null, TEST_EXPIRY_DATE)
        );
        assertEquals("Document ID mismatch", exception.getMessage());
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void testVerifyKycRecoveryFields_EmptyIdNumber() {
        // Arrange
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(personalInfo.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, "", TEST_EXPIRY_DATE)
        );
        assertEquals("Document ID mismatch", exception.getMessage());
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void testVerifyKycRecoveryFields_NullExpiryDate() {
        // Arrange
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(personalInfo.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, TEST_ID_NUMBER, null)
        );
        assertEquals("Document expiry date mismatch", exception.getMessage());
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void testVerifyKycRecoveryFields_EmptyExpiryDate() {
        // Arrange
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(personalInfo.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, TEST_ID_NUMBER, "")
        );
        assertEquals("Document expiry date mismatch", exception.getMessage());
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void testVerifyKycRecoveryFields_AccountNotFound() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE)
        );
        assertEquals("No record found for accountId " + TEST_ACCOUNT_ID, exception.getMessage());
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void testVerifyKycRecoveryFields_IdNumberMismatch() {
        // Arrange
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn("different-id-number");
        when(personalInfo.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE)
        );
        assertEquals("Document ID mismatch", exception.getMessage());
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void testVerifyKycRecoveryFields_ExpiryDateMismatch() {
        // Arrange
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(personalInfo.getExpirationDate()).thenReturn("2024-12-31");

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE)
        );
        assertEquals("Document expiry date mismatch", exception.getMessage());
        verify(personalInfoRepository).findByAccountId(TEST_ACCOUNT_ID);
    }
}