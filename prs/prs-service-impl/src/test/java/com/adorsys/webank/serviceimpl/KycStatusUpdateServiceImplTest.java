package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.error.ValidationException;
import com.adorsys.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycStatusUpdateServiceImplTest {

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @InjectMocks
    private KycStatusUpdateServiceImpl kycStatusUpdateService;

    private PersonalInfoProjection dummyProjection;
    private static final String TEST_ACCOUNT_ID = "test-account-id";
    private static final String TEST_ID_NUMBER = "test-id-number";
    private static final String TEST_EXPIRY_DATE = "2025-12-31";
    private static final String TEST_REJECTION_REASON = "Invalid documents";

    @BeforeEach
    void setUp() {
        dummyProjection = mock(PersonalInfoProjection.class);
        when(dummyProjection.getAccountId()).thenReturn(TEST_ACCOUNT_ID);
        when(dummyProjection.getStatus()).thenReturn(PersonalInfoStatus.PENDING);
        when(dummyProjection.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(dummyProjection.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);
    }

    @Test
    void testUpdateKycStatus_Success() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(dummyProjection));
        when(personalInfoRepository.save(any(PersonalInfoEntity.class))).thenReturn(new PersonalInfoEntity());

        // Act
        String response = kycStatusUpdateService.updateKycStatus(
            TEST_ACCOUNT_ID, "APPROVED", TEST_ID_NUMBER, TEST_EXPIRY_DATE, null);

        // Assert
        assertEquals("KYC status for " + TEST_ACCOUNT_ID + " updated to APPROVED", response);
        verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_RejectedWithReason() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(dummyProjection));
        when(personalInfoRepository.save(any(PersonalInfoEntity.class))).thenReturn(new PersonalInfoEntity());

        // Act
        String response = kycStatusUpdateService.updateKycStatus(
            TEST_ACCOUNT_ID, "REJECTED", TEST_ID_NUMBER, TEST_EXPIRY_DATE, TEST_REJECTION_REASON);

        // Assert
        assertEquals("KYC status for " + TEST_ACCOUNT_ID + " updated to REJECTED", response);
        verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_RejectedWithoutReason() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(dummyProjection));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycStatusUpdateService.updateKycStatus(
                TEST_ACCOUNT_ID, "REJECTED", TEST_ID_NUMBER, TEST_EXPIRY_DATE, null)
        );
        assertEquals("Rejection reason is required when status is REJECTED", exception.getMessage());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_NullAccountId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycStatusUpdateService.updateKycStatus(
                null, "APPROVED", TEST_ID_NUMBER, TEST_EXPIRY_DATE, null)
        );
        assertEquals("Account ID is required", exception.getMessage());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_NullStatus() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycStatusUpdateService.updateKycStatus(
                TEST_ACCOUNT_ID, null, TEST_ID_NUMBER, TEST_EXPIRY_DATE, null)
        );
        assertEquals("Status is required", exception.getMessage());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_NullIdNumber() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycStatusUpdateService.updateKycStatus(
                TEST_ACCOUNT_ID, "APPROVED", null, TEST_EXPIRY_DATE, null)
        );
        assertEquals("ID number is required", exception.getMessage());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_NullExpiryDate() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycStatusUpdateService.updateKycStatus(
                TEST_ACCOUNT_ID, "APPROVED", TEST_ID_NUMBER, null, null)
        );
        assertEquals("Expiry date is required", exception.getMessage());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_InvalidStatus() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(dummyProjection));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycStatusUpdateService.updateKycStatus(
                TEST_ACCOUNT_ID, "INVALID_STATUS", TEST_ID_NUMBER, TEST_EXPIRY_DATE, null)
        );
        assertEquals("Invalid KYC status value 'INVALID_STATUS'", exception.getMessage());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_RecordNotFound() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            kycStatusUpdateService.updateKycStatus(
                TEST_ACCOUNT_ID, "APPROVED", TEST_ID_NUMBER, TEST_EXPIRY_DATE, null)
        );
        assertEquals("No record found for accountId " + TEST_ACCOUNT_ID, exception.getMessage());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_DocumentIdMismatch() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(dummyProjection));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycStatusUpdateService.updateKycStatus(
                TEST_ACCOUNT_ID, "APPROVED", "wrong-id-number", TEST_EXPIRY_DATE, null)
        );
        assertEquals("Document ID mismatch", exception.getMessage());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_ExpiryDateMismatch() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(dummyProjection));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            kycStatusUpdateService.updateKycStatus(
                TEST_ACCOUNT_ID, "APPROVED", TEST_ID_NUMBER, "2024-12-31", null)
        );
        assertEquals("Document expiry date mismatch", exception.getMessage());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }
}