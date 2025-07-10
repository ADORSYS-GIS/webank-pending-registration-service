package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.dto.response.KycStatusUpdateResponse;
import com.adorsys.webank.repository.PersonalInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        when(personalInfoRepository.save(any(PersonalInfoEntity.class))).thenReturn(dummyEntity);

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
            accountId, "APPROVED", idNumber, expiryDate, null);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("APPROVED", response.getStatus());
        assertEquals(accountId, response.getAccountId());
        assertTrue(response.getMessage().contains("successfully"));
        verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_Success_Rejected() {
        // Given
        String rejectionReason = "Document is blurry";
        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(dummyEntity));
        when(personalInfoRepository.save(any(PersonalInfoEntity.class))).thenReturn(dummyEntity);

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
            accountId, "REJECTED", idNumber, expiryDate, rejectionReason);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("REJECTED", response.getStatus());
        assertEquals(accountId, response.getAccountId());
        assertTrue(response.getMessage().contains("successfully"));
        verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_Rejected_MissingReason() {
        // Given
        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(dummyEntity));

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
            accountId, "REJECTED", idNumber, expiryDate, null);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("REJECTED", response.getStatus());
        assertEquals("Rejection reason is required for REJECTED status", response.getMessage());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_InvalidStatus() {
        // Given
        String invalidStatus = "INVALID_STATUS";
        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(dummyEntity));

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
            accountId, invalidStatus, idNumber, expiryDate, null);

        // Then
        assertFalse(response.isSuccess());
        assertEquals(invalidStatus, response.getStatus());
        assertTrue(response.getMessage().contains("Invalid status value"));
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_RecordNotFound() {
        // Given
        String nonExistingAccountId = "non-existing-account-id";
        when(personalInfoRepository.findById(nonExistingAccountId)).thenReturn(Optional.empty());

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
            nonExistingAccountId, "APPROVED", idNumber, expiryDate, null);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("APPROVED", response.getStatus());
        assertTrue(response.getMessage().contains("No KYC record found"));
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_DocumentIdMismatch() {
        // Given
        String wrongIdNumber = "wrong-id-number";
        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(dummyEntity));

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
            accountId, "APPROVED", wrongIdNumber, expiryDate, null);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("APPROVED", response.getStatus());
        assertTrue(response.getMessage().contains("Document ID mismatch"));
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_ExpiryDateMismatch() {
        // Given
        String wrongExpiryDate = "2024-12-31";
        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(dummyEntity));

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
            accountId, "APPROVED", idNumber, wrongExpiryDate, null);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("APPROVED", response.getStatus());
        assertTrue(response.getMessage().contains("Document expiry date mismatch"));
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }
    
    @Test
    void testUpdateKycStatus_ExceptionHandling() {
        // Given
        when(personalInfoRepository.findById(accountId)).thenThrow(new RuntimeException("Database error"));

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
            accountId, "APPROVED", idNumber, expiryDate, null);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("APPROVED", response.getStatus());
        assertTrue(response.getMessage().contains("Failed to update KYC status"));
        assertTrue(response.getMessage().contains("Database error"));
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }
}