package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
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
    void verifyKycRecoveryFields_Success() {
        // Given
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(personalInfo.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // When
        String result = kycRecoveryService.verifyKycRecoveryFields(
                TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE);

        // Then
        assertEquals("Document verification successful", result);
        verify(personalInfoRepository, times(1)).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void verifyKycRecoveryFields_NoRecordFound() {
        // Given
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        // When
        String result = kycRecoveryService.verifyKycRecoveryFields(
                TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE);

        // Then
        assertEquals("Failed: No record found for accountId " + TEST_ACCOUNT_ID, result);
        verify(personalInfoRepository, times(1)).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void verifyKycRecoveryFields_DocumentIdMismatch() {
        // Given
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn("different-id");
        when(personalInfo.getExpirationDate()).thenReturn(TEST_EXPIRY_DATE);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // When
        String result = kycRecoveryService.verifyKycRecoveryFields(
                TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE);

        // Then
        assertEquals("Failed: Document ID mismatch", result);
        verify(personalInfoRepository, times(1)).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    void verifyKycRecoveryFields_ExpiryDateMismatch() {
        // Given
        PersonalInfoProjection personalInfo = mock(PersonalInfoProjection.class);
        when(personalInfo.getDocumentUniqueId()).thenReturn(TEST_ID_NUMBER);
        when(personalInfo.getExpirationDate()).thenReturn("2024-12-31");

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(personalInfo));

        // When
        String result = kycRecoveryService.verifyKycRecoveryFields(
                TEST_ACCOUNT_ID, TEST_ID_NUMBER, TEST_EXPIRY_DATE);

        // Then
        assertEquals("Failed: Document expiry date mismatch", result);
        verify(personalInfoRepository, times(1)).findByAccountId(TEST_ACCOUNT_ID);
    }
}