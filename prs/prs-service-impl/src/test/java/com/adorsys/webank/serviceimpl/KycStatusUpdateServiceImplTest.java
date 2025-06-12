package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.repository.PersonalInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycStatusUpdateServiceImplTest {

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @InjectMocks
    private KycStatusUpdateServiceImpl kycStatusUpdateServiceImpl;

    private PersonalInfoEntity dummyEntity;
    private static final String accountId = "dummy-account-id";
    private static final String idNumber = "dummy-id-number";
    private static final String expiryDate = "2025-12-31";


    @BeforeEach
    void setUp() {
        dummyEntity = new PersonalInfoEntity();
        dummyEntity.setAccountId(accountId);
        dummyEntity.setStatus(PersonalInfoStatus.PENDING);
        dummyEntity.setDocumentUniqueId(idNumber);
        dummyEntity.setExpirationDate(expiryDate);
        MDC.put("correlationId", "test-correlation-id");
    }

    @Test
    void testUpdateKycStatus_Success_Approved() {
        // Given
        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(dummyEntity));

        // When
        String response = kycStatusUpdateServiceImpl.updateKycStatus(accountId, "APPROVED", idNumber, expiryDate, null);

        // Then
        assertEquals("KYC status updated successfully to APPROVED", response);
        assertEquals(PersonalInfoStatus.APPROVED, dummyEntity.getStatus());
        assertNull(dummyEntity.getRejectionReason());
        verify(personalInfoRepository).save(dummyEntity);
    }

    @Test
    void testUpdateKycStatus_Success_Rejected() {
        // Given
        String rejectionReason = "Document is blurry";
        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(dummyEntity));

        // When
        String response = kycStatusUpdateServiceImpl.updateKycStatus(accountId, "REJECTED", idNumber, expiryDate, rejectionReason);

        // Then
        assertEquals("KYC status updated successfully to REJECTED", response);
        assertEquals(PersonalInfoStatus.REJECTED, dummyEntity.getStatus());
        assertEquals(rejectionReason, dummyEntity.getRejectionReason());
        verify(personalInfoRepository).save(dummyEntity);
    }

    @Test
    void testUpdateKycStatus_Rejected_MissingReason() {
        // Given
        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(dummyEntity));

        // When
        String response = kycStatusUpdateServiceImpl.updateKycStatus(accountId, "REJECTED", idNumber, expiryDate, null);

        // Then
        assertEquals("Failed: Rejection reason is required when status is REJECTED", response);
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_InvalidStatus() {
        // Given
        String invalidStatus = "notAValidStatus";
        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(dummyEntity));

        // When
        String response = kycStatusUpdateServiceImpl.updateKycStatus(accountId, invalidStatus, idNumber, expiryDate, null);

        // Then
        assertEquals("Failed: Invalid KYC status value '" + invalidStatus + "'", response);
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_RecordNotFound() {
        // Given
        String nonExistingAccountId = "non-existing-account-id";
        when(personalInfoRepository.findById(nonExistingAccountId)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(EntityNotFoundException.class, () ->
                kycStatusUpdateServiceImpl.updateKycStatus(nonExistingAccountId, "APPROVED", idNumber, expiryDate, null));
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_DocumentIdMismatch() {
        // Given
        String wrongIdNumber = "wrong-id-number";
        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(dummyEntity));

        // When
        String response = kycStatusUpdateServiceImpl.updateKycStatus(accountId, "APPROVED", wrongIdNumber, expiryDate, null);

        // Then
        assertEquals("Failed: Document ID mismatch", response);
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_ExpiryDateMismatch() {
        // Given
        String wrongExpiryDate = "2024-12-31";
        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(dummyEntity));

        // When
        String response = kycStatusUpdateServiceImpl.updateKycStatus(accountId, "APPROVED", idNumber, wrongExpiryDate, null);

        // Then
        assertEquals("Failed: Document expiry date mismatch", response);
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }
}