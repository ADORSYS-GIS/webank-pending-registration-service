package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.repository.PersonalInfoRepository;
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
    private KycStatusUpdateServiceImpl kycStatusUpdateServiceImpl;

    private PersonalInfoEntity dummyEntity;

    @BeforeEach
    void setUp() {
        dummyEntity = new PersonalInfoEntity();
        dummyEntity.setAccountId("dummyHash");
        dummyEntity.setStatus(PersonalInfoStatus.PENDING);
        dummyEntity.setDocumentUniqueId("dummyIdNumber");
        dummyEntity.setExpirationDate("2025-12-31");
    }

    @Test
    void testUpdateKycStatus_Success() {
        // Given a valid status string and matching document details
        when(personalInfoRepository.findByAccountId("dummyHash")).thenReturn(Optional.of(dummyEntity));
        when(personalInfoRepository.save(any(PersonalInfoEntity.class))).thenReturn(dummyEntity);

        String newStatus = "approved";
        String idNumber = "dummyIdNumber";
        String expiryDate = "2025-12-31";

        String response = kycStatusUpdateServiceImpl.updateKycStatus("dummyHash", newStatus, idNumber, expiryDate);

        assertEquals("KYC status for dummyHash updated to approved", response);
        assertEquals(PersonalInfoStatus.APPROVED, dummyEntity.getStatus());
        verify(personalInfoRepository, times(1)).save(dummyEntity);
    }

    @Test
    void testUpdateKycStatus_InvalidStatus() {
        // Given an invalid status string
        when(personalInfoRepository.findByAccountId("dummyHash")).thenReturn(Optional.of(dummyEntity));

        String invalidStatus = "notAValidStatus";
        String idNumber = "dummyIdNumber";
        String expiryDate = "2025-12-31";

        String response = kycStatusUpdateServiceImpl.updateKycStatus("dummyHash", invalidStatus, idNumber, expiryDate);

        assertEquals("Failed: Invalid KYC status value 'notAValidStatus'", response);
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_RecordNotFound() {
        // Given no record found for the provided accountId
        when(personalInfoRepository.findByAccountId("nonExistingHash")).thenReturn(Optional.empty());

        String response = kycStatusUpdateServiceImpl.updateKycStatus("nonExistingHash", "approved", "dummyIdNumber", "2025-12-31");

        assertEquals("Failed: No record found for accountId nonExistingHash", response);
    }

    @Test
    void testUpdateKycStatus_DocumentIdMismatch() {
        // Given a mismatched document ID
        when(personalInfoRepository.findByAccountId("dummyHash")).thenReturn(Optional.of(dummyEntity));

        String newStatus = "approved";
        String idNumber = "wrongIdNumber";
        String expiryDate = "2025-12-31";

        String response = kycStatusUpdateServiceImpl.updateKycStatus("dummyHash", newStatus, idNumber, expiryDate);

        assertEquals("Failed: Document ID mismatch", response);
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }

    @Test
    void testUpdateKycStatus_ExpiryDateMismatch() {
        // Given a mismatched expiry date
        when(personalInfoRepository.findByAccountId("dummyHash")).thenReturn(Optional.of(dummyEntity));

        String newStatus = "approved";
        String idNumber = "dummyIdNumber";
        String expiryDate = "2024-12-31";

        String response = kycStatusUpdateServiceImpl.updateKycStatus("dummyHash", newStatus, idNumber, expiryDate);

        assertEquals("Failed: Document expiry date mismatch", response);
        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
    }
}