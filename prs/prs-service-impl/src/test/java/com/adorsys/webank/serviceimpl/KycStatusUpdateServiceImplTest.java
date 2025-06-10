package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;

@ExtendWith(MockitoExtension.class)
class KycStatusUpdateServiceImplTest {

    @Mock
    private PersonalInfoRepository inforepository;

    @InjectMocks
    private KycStatusUpdateServiceImpl kycStatusUpdateService;

    private static final String TEST_ACCOUNT_ID = "ACC123456";
    private static final String TEST_ID_NUMBER = "ID-987654";
    private static final String TEST_EXPIRY_DATE = "2025-12-31";
    private static final String CORRELATION_ID = "test-correlation-id";

    @BeforeEach
    void setUp() {
        MDC.put("correlationId", CORRELATION_ID);
    }

    @Test
    void updateKycStatus_SuccessApproved() {
        // Arrange
        PersonalInfoProjection projection = createValidProjection();
        when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(projection));
        
        // Act
        String result = kycStatusUpdateService.updateKycStatus(
            TEST_ACCOUNT_ID, 
            "approved", 
            TEST_ID_NUMBER, 
            TEST_EXPIRY_DATE, 
            null
        );
        
        // Assert
        assertEquals("KYC status for " + TEST_ACCOUNT_ID + " updated to approved", result);
        verify(inforepository).save(any(PersonalInfoEntity.class));
    }

    @Test
    void updateKycStatus_SuccessRejectedWithReason() {
        // Arrange
        PersonalInfoProjection projection = createValidProjection();
        when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(projection));
        
        // Act
        String result = kycStatusUpdateService.updateKycStatus(
            TEST_ACCOUNT_ID, 
            "rejected", 
            TEST_ID_NUMBER, 
            TEST_EXPIRY_DATE, 
            "Document quality poor"
        );
        
        // Assert
        assertEquals("KYC status for " + TEST_ACCOUNT_ID + " updated to rejected", result);
        verify(inforepository).save(any(PersonalInfoEntity.class));
    }

    @Test
    void updateKycStatus_AccountNotFound() {
        // Arrange
        when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.empty());
        
        // Act
        String result = kycStatusUpdateService.updateKycStatus(
            TEST_ACCOUNT_ID, 
            "approved", 
            TEST_ID_NUMBER, 
            TEST_EXPIRY_DATE, 
            null
        );
        
        // Assert
        assertEquals("Failed: No record found for accountId " + TEST_ACCOUNT_ID, result);
        verify(inforepository, never()).save(any());
    }

    // @Test
    // void updateKycStatus_DocumentIdMismatch() {
    //     // Arrange
    //     PersonalInfoProjection projection = createValidProjection();
    //     when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
    //         .thenReturn(Optional.of(projection));
        
    //     // Act
    //     String result = kycStatusUpdateService.updateKycStatus(
    //         TEST_ACCOUNT_ID, 
    //         "approved", 
    //         "WRONG-ID", 
    //         TEST_EXPIRY_DATE, 
    //         null
    //     );
        
    //     // Assert
    //     assertEquals("Failed: Document ID mismatch", result);
    //     verify(inforepository, never()).save(any());
    // }

    @Test
    void updateKycStatus_ExpiryDateMismatch() {
        // Arrange
        PersonalInfoProjection projection = createValidProjection();
        when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(projection));
        
        // Act
        String result = kycStatusUpdateService.updateKycStatus(
            TEST_ACCOUNT_ID, 
            "approved", 
            TEST_ID_NUMBER, 
            "2024-01-01", 
            null
        );
        
        // Assert
        assertEquals("Failed: Document expiry date mismatch", result);
        verify(inforepository, never()).save(any());
    }

    @Test
    void updateKycStatus_RejectedWithoutReason() {
        // Arrange
        PersonalInfoProjection projection = createValidProjection();
        when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(projection));
        
        // Act
        String result = kycStatusUpdateService.updateKycStatus(
            TEST_ACCOUNT_ID, 
            "rejected", 
            TEST_ID_NUMBER, 
            TEST_EXPIRY_DATE, 
            null
        );
        
        // Assert
        assertEquals("Failed: Rejection reason is required when status is REJECTED", result);
        verify(inforepository, never()).save(any());
    }

    @Test
    void updateKycStatus_RejectedWithEmptyReason() {
        // Arrange
        PersonalInfoProjection projection = createValidProjection();
        when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(projection));
        
        // Act
        String result = kycStatusUpdateService.updateKycStatus(
            TEST_ACCOUNT_ID, 
            "rejected", 
            TEST_ID_NUMBER, 
            TEST_EXPIRY_DATE, 
            "   "
        );
        
        // Assert
      // @Test
    // void updateKycStatus_ClearsRejectionReasonWhenNotRejected() {
    //     // Arrange
    //     PersonalInfoProjection projection = createValidProjection();
    //     when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
    //         .thenReturn(Optional.of(projection));
        
    //     // Create an entity with existing rejection reason
    //     PersonalInfoEntity existingEntity = new PersonalInfoEntity();
    //     existingEntity.setRejectionReason("Previous rejection");
        
    //     // Act
    //     kycStatusUpdateService.updateKycStatus(
    //         TEST_ACCOUNT_ID, 
    //         "approved", 
    //         TEST_ID_NUMBER, 
    //         TEST_EXPIRY_DATE, 
    //         null
    //     );
        
    //     // Assert
    //     verify(inforepository).save(any(entity -> {
    //         assertEquals(PersonalInfoStatus.APPROVED, entity.getStatus());
    //         assertEquals(null, entity.getRejectionReason());
    //     }));
    // }

    // @Test
    // void updateKycStatus_StatusCaseInsensitive() {
    //     // Arrange
    //     PersonalInfoProjection projection = createValidProjection();
    //     when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
    //         .thenReturn(Optional.of(projection));
        
    //     // Act
    //     String result = kycStatusUpdateService.updateKycStatus(
    //         TEST_ACCOUNT_ID, 
    //         "ApPrOvEd", 
    //         TEST_ID_NUMBER, 
    //         TEST_EXPIRY_DATE, 
    //         null
    //     );
        
    //     // Assert
    //     assertEquals("KYC status for " + TEST_ACCOUNT_ID + " updated to ApPrOvEd", result);
    //     verify(inforepository).save(any(entity -> 
    //         assertEquals(PersonalInfoStatus.APPROVED, entity.getStatus())
    //     ));
    // }

    // @Test
    // void maskAccountId_ValidInput() {
    //     // Arrange
    //     KycStatusUpdateServiceImpl service = new KycStatusUpdateServiceImpl(null);
        
    //     // Act & Assert
    //     assertEquals("AC****56", service.maskAccountId("ACC123456"));
    //     assertEquals("12****90", service.maskAccountId("1234567890"));
    // }

    // @Test
    // void maskAccountId_ShortInput() {
    //     // Arrange
    //     KycStatusUpdateServiceImpl service = new KycStatusUpdateServiceImpl(null);
        
    //     // Act & Assert
    //     assertEquals("********", service.maskAccountId("123"));
    //     assertEquals("A****B", service.maskAccountId("AB"));
    //     assertEquals("********", service.maskAccountId(""));
    //     assertEquals("********", service.maskAccountId(null));
    // }   assertEquals("Failed: Rejection reason is required when status is REJECTED", result);
        verify(inforepository, never()).save(any());
    }

    @Test
    void updateKycStatus_InvalidStatusValue() {
        // Arrange
        PersonalInfoProjection projection = createValidProjection();
        when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(projection));
        
        // Act
        String result = kycStatusUpdateService.updateKycStatus(
            TEST_ACCOUNT_ID, 
            "invalid_status", 
            TEST_ID_NUMBER, 
            TEST_EXPIRY_DATE, 
            null
        );
        
        // Assert
        assertEquals("Failed: Invalid KYC status value 'invalid_status'", result);
        verify(inforepository, never()).save(any());
    }

    // @Test
    // void updateKycStatus_ClearsRejectionReasonWhenNotRejected() {
    //     // Arrange
    //     PersonalInfoProjection projection = createValidProjection();
    //     when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
    //         .thenReturn(Optional.of(projection));
        
    //     // Create an entity with existing rejection reason
    //     PersonalInfoEntity existingEntity = new PersonalInfoEntity();
    //     existingEntity.setRejectionReason("Previous rejection");
        
    //     // Act
    //     kycStatusUpdateService.updateKycStatus(
    //         TEST_ACCOUNT_ID, 
    //         "approved", 
    //         TEST_ID_NUMBER, 
    //         TEST_EXPIRY_DATE, 
    //         null
    //     );
        
    //     // Assert
    //     verify(inforepository).save(any(entity -> {
    //         assertEquals(PersonalInfoStatus.APPROVED, entity.getStatus());
    //         assertEquals(null, entity.getRejectionReason());
    //     }));
    // }

    // @Test
    // void updateKycStatus_StatusCaseInsensitive() {
    //     // Arrange
    //     PersonalInfoProjection projection = createValidProjection();
    //     when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
    //         .thenReturn(Optional.of(projection));
        
    //     // Act
    //     String result = kycStatusUpdateService.updateKycStatus(
    //         TEST_ACCOUNT_ID, 
    //         "ApPrOvEd", 
    //         TEST_ID_NUMBER, 
    //         TEST_EXPIRY_DATE, 
    //         null
    //     );
        
    //     // Assert
    //     assertEquals("KYC status for " + TEST_ACCOUNT_ID + " updated to ApPrOvEd", result);
    //     verify(inforepository).save(any(entity -> 
    //         assertEquals(PersonalInfoStatus.APPROVED, entity.getStatus())
    //     ));
    // }

    // @Test
    // void maskAccountId_ValidInput() {
    //     // Arrange
    //     KycStatusUpdateServiceImpl service = new KycStatusUpdateServiceImpl(null);
        
    //     // Act & Assert
    //     assertEquals("AC****56", service.maskAccountId("ACC123456"));
    //     assertEquals("12****90", service.maskAccountId("1234567890"));
    // }

    // @Test
    // void maskAccountId_ShortInput() {
    //     // Arrange
    //     KycStatusUpdateServiceImpl service = new KycStatusUpdateServiceImpl(null);
        
    //     // Act & Assert
    //     assertEquals("********", service.maskAccountId("123"));
    //     assertEquals("A****B", service.maskAccountId("AB"));
    //     assertEquals("********", service.maskAccountId(""));
    //     assertEquals("********", service.maskAccountId(null));
    // }

    @Test
    void updateKycStatus_ExceptionDuringUpdate() {
        // Arrange
        PersonalInfoProjection projection = createValidProjection();
        when(inforepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(projection));
        
        when(inforepository.save(any())).thenThrow(new RuntimeException("DB error"));
        
        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            kycStatusUpdateService.updateKycStatus(
                TEST_ACCOUNT_ID, 
                "approved", 
                TEST_ID_NUMBER, 
                TEST_EXPIRY_DATE, 
                null
            )
        );
    }

    private PersonalInfoProjection createValidProjection() {
        PersonalInfoProjection projection = mock(PersonalInfoProjection.class);
        when(projection.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(projection.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);
        return projection;
    }
}