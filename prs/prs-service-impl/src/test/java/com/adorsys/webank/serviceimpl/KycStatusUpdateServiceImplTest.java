//package com.adorsys.webank.serviceimpl;
//
//import com.adorsys.webank.domain.PersonalInfoEntity;
//import com.adorsys.webank.domain.PersonalInfoStatus;
//import com.adorsys.webank.repository.PersonalInfoRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class KycStatusUpdateServiceImplTest {
//
//    @Mock
//    private PersonalInfoRepository personalInfoRepository;
//
//    @InjectMocks
//    private KycStatusUpdateServiceImpl kycStatusUpdateServiceImpl;
//
//    private PersonalInfoEntity dummyEntity;
//
//    @BeforeEach
//    void setUp() {
//        dummyEntity = new PersonalInfoEntity();
//        dummyEntity.setPublicKeyHash("dummyHash");
//        dummyEntity.setStatus(PersonalInfoStatus.PENDING); // default initial status
//    }
//
//    @Test
//    void testUpdateKycStatus_Success() {
//        // Given a valid status string that converts to an enum (e.g., APPROVED)
//        when(personalInfoRepository.findByPublicKeyHash("dummyHash")).thenReturn(Optional.of(dummyEntity));
//        when(personalInfoRepository.save(any(PersonalInfoEntity.class))).thenReturn(dummyEntity);
//
//        String newStatus = "approved";  // will be converted to APPROVED by valueOf(…)
//        String response = kycStatusUpdateServiceImpl.updateKycStatus("dummyHash", newStatus);
//
//        assertEquals("KYC status for dummyHash updated to approved", response);
//        assertEquals(PersonalInfoStatus.APPROVED, dummyEntity.getStatus());
//        verify(personalInfoRepository, times(1)).save(dummyEntity);
//    }
//
//    @Test
//    void testUpdateKycStatus_InvalidStatus() {
//        // Given an invalid status string which should trigger IllegalArgumentException in valueOf(…)
//        when(personalInfoRepository.findByPublicKeyHash("dummyHash")).thenReturn(Optional.of(dummyEntity));
//
//        String invalidStatus = "notAValidStatus";
//        String response = kycStatusUpdateServiceImpl.updateKycStatus("dummyHash", invalidStatus);
//
//        assertEquals("Failed: Invalid KYC status value 'notAValidStatus'", response);
//        // Ensure save is not attempted when status conversion fails.
//        verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
//    }
//
//    @Test
//    void testUpdateKycStatus_RecordNotFound() {
//        // Given no record found for the provided publicKeyHash.
//        when(personalInfoRepository.findByPublicKeyHash("nonExistingHash")).thenReturn(Optional.empty());
//
//        String response = kycStatusUpdateServiceImpl.updateKycStatus("nonExistingHash", "approved");
//
//        assertEquals("Failed: No record found for publicKeyHash nonExistingHash", response);
//    }
//
//    @Test
//    void testGetPersonalInfoByPublicKey() {
//        when(personalInfoRepository.findByPublicKeyHash("dummyHash")).thenReturn(Optional.of(dummyEntity));
//        Optional<PersonalInfoEntity> result = kycStatusUpdateServiceImpl.getPersonalInfoByPublicKey("dummyHash");
//
//        assertTrue(result.isPresent());
//        assertEquals("dummyHash", result.get().getPublicKeyHash());
//    }
//}
