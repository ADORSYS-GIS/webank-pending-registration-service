package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.error.ValidationException;
import com.adorsys.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setDocumentUniqueId(TEST_ID_NUMBER);
        personalInfo.setExpirationDate(TEST_EXPIRY_DATE);

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
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(null, TEST_ID_NUMBER, TEST_EXPIRY_DATE)
        );
        assertEquals("Account ID is required", exception.getMessage());
        verify(personalInfoRepository, never()).findByAccountId(anyString());
    }

    @Test
    void testVerifyKycRecoveryFields_EmptyAccountId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields("", TEST_ID_NUMBER, TEST_EXPIRY_DATE)
        );
        assertEquals("Account ID is required", exception.getMessage());
        verify(personalInfoRepository, never()).findByAccountId(anyString());
    }

    @Test
    void testVerifyKycRecoveryFields_NullIdNumber() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, null, TEST_EXPIRY_DATE)
        );
        assertEquals("ID number is required", exception.getMessage());
        verify(personalInfoRepository, never()).findByAccountId(anyString());
    }

    @Test
    void testVerifyKycRecoveryFields_EmptyIdNumber() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, "", TEST_EXPIRY_DATE)
        );
        assertEquals("ID number is required", exception.getMessage());
        verify(personalInfoRepository, never()).findByAccountId(anyString());
    }

    @Test
    void testVerifyKycRecoveryFields_NullExpiryDate() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, TEST_ID_NUMBER, null)
        );
        assertEquals("Expiry date is required", exception.getMessage());
        verify(personalInfoRepository, never()).findByAccountId(anyString());
    }

    @Test
    void testVerifyKycRecoveryFields_EmptyExpiryDate() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, TEST_ID_NUMBER, "")
        );
        assertEquals("Expiry date is required", exception.getMessage());
        verify(personalInfoRepository, never()).findByAccountId(anyString());
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
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setDocumentUniqueId("different-id-number");
        personalInfo.setExpirationDate(TEST_EXPIRY_DATE);

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
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setDocumentUniqueId(TEST_ID_NUMBER);
        personalInfo.setExpirationDate("2024-12-31");

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