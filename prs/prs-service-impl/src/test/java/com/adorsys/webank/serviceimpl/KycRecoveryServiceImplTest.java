package com.adorsys.webank.serviceimpl;


import com.adorsys.webank.exceptions.KycProcessingException;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void testVerifyKycRecoveryFieldsSuccess() {
        // Given
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(personalInfo.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // When
        Boolean result = kycRecoveryService.verifyKycRecoveryFields(
                TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE);

        // Then
        assertEquals(true, result);
        verify(personalInfoRepository, times(1)).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void testVerifyKycRecoveryFieldsNoRecordFound() {
        // Given
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        // When & Then
        KycProcessingException exception = assertThrows(KycProcessingException.class, () -> 
                kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE));
        
        // Verify the exception message
        assertEquals("No record found for accountId " + TEST_ACCOUNT_ID, exception.getMessage());
        verify(personalInfoRepository, times(1)).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void testVerifyKycRecoveryFieldsDocumentIdMismatch() {
        // Given
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn("different-id");
        when(personalInfo.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // When & Then
        KycProcessingException exception = assertThrows(KycProcessingException.class, () -> 
                kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE));
        
        // Verify the exception message
        assertEquals("Document ID mismatch", exception.getMessage());
        verify(personalInfoRepository, times(1)).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void testVerifyKycRecoveryFieldsExpiryDateMismatch() {
        // Given
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(personalInfo.getExpirationDate()).thenReturn("2024-12-31");

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // When & Then
        KycProcessingException exception = assertThrows(KycProcessingException.class, () -> 
                kycRecoveryService.verifyKycRecoveryFields(TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE));
        
        // Verify the exception message
        assertEquals("Document expiry date mismatch", exception.getMessage());
        verify(personalInfoRepository, times(1)).findByAccountId(TEST_ACCOUNT_ID);
    }
}