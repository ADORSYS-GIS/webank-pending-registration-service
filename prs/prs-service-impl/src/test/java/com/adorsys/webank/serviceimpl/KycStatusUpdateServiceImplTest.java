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
    private static final String ACCOUNT_ID = "dummy-account-id";
    private static final String ID_NUMBER = "dummy-id-number";
    private static final String EXPIRY_DATE = "2025-12-31";


    @BeforeEach
    void setUp() {
        dummyEntity = new PersonalInfoEntity();
        dummyEntity.setAccountId(ACCOUNT_ID);
        dummyEntity.setStatus(PersonalInfoStatus.PENDING);
        dummyEntity.setDocumentUniqueId(ID_NUMBER);
        dummyEntity.setExpirationDate(EXPIRY_DATE);
        MDC.put("correlationId", "test-correlation-id");
    }

    @Test
    void testUpdateKycStatus_Success_Approved() {
        // Given
        when(personalInfoRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(dummyEntity));

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
                ACCOUNT_ID, "APPROVED", ID_NUMBER, EXPIRY_DATE, null);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("APPROVED", response.getStatus());
        assertTrue(response.getMessage().contains("successfully"));
        assertEquals(ACCOUNT_ID, response.getAccountId());
        assertEquals(PersonalInfoStatus.APPROVED, dummyEntity.getStatus());
        assertNull(dummyEntity.getRejectionReason());
        verify(personalInfoRepository, times(1)).save(dummyEntity);
    }

    @Test
    void testUpdateKycStatus_Success_Rejected() {
        // Given
        String rejectionReason = "Document quality is poor";
        when(personalInfoRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(dummyEntity));

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
                ACCOUNT_ID, "REJECTED", ID_NUMBER, EXPIRY_DATE, rejectionReason);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("REJECTED", response.getStatus());
        assertTrue(response.getMessage().contains("REJECTED"));
        assertEquals(ACCOUNT_ID, response.getAccountId());
        assertEquals(ID_NUMBER, response.getDocumentId());
        assertEquals(PersonalInfoStatus.REJECTED, dummyEntity.getStatus());
        assertEquals(rejectionReason, dummyEntity.getRejectionReason());
        verify(personalInfoRepository, times(1)).save(dummyEntity);
    }

    @Test
    void testUpdateKycStatus_Rejected_MissingReason() {
        // Given
        when(personalInfoRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(dummyEntity));

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
                ACCOUNT_ID, "REJECTED", ID_NUMBER, EXPIRY_DATE, null);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Rejection reason is required"));
        assertEquals(ACCOUNT_ID, response.getAccountId());
        assertEquals(ID_NUMBER, response.getDocumentId());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_InvalidStatus() {
        // Given
        String invalidStatus = "notAValidStatus";
        when(personalInfoRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(dummyEntity));

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
                ACCOUNT_ID, invalidStatus, ID_NUMBER, EXPIRY_DATE, null);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Invalid KYC status provided: " + invalidStatus));
        assertEquals(ACCOUNT_ID, response.getAccountId());
        assertEquals(ID_NUMBER, response.getDocumentId());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_RecordNotFound() {
        // Given
        String nonExistingAccountId = "non-existing-account-id";
        when(personalInfoRepository.findById(nonExistingAccountId)).thenReturn(Optional.empty());

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
                nonExistingAccountId, "APPROVED", ID_NUMBER, EXPIRY_DATE, null);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("An error occurred while processing your request: No KYC record found for accountId: " + nonExistingAccountId.substring(0, 2) + "****" + nonExistingAccountId.substring(nonExistingAccountId.length() - 2)));
        assertEquals(nonExistingAccountId, response.getAccountId());
        assertEquals(ID_NUMBER, response.getDocumentId());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_DocumentIdMismatch() {
        // Given
        String wrongIdNumber = "wrong-id-number";
        when(personalInfoRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(dummyEntity));

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
                ACCOUNT_ID, "APPROVED", wrongIdNumber, EXPIRY_DATE, null);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Document verification failed: ID number does not match records"));
        assertEquals(ACCOUNT_ID, response.getAccountId());
        assertEquals(wrongIdNumber, response.getDocumentId());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_ExpiryDateMismatch() {
        // Given
        String wrongExpiryDate = "2024-12-31";
        when(personalInfoRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(dummyEntity));

        // When
        KycStatusUpdateResponse response = kycStatusUpdateServiceImpl.updateKycStatus(
                ACCOUNT_ID, "APPROVED", ID_NUMBER, wrongExpiryDate, null);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Document verification failed: Expiry date does not match records"));
        assertEquals(ACCOUNT_ID, response.getAccountId());
        assertEquals(ID_NUMBER, response.getDocumentId());
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }
}