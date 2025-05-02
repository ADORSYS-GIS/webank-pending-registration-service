//package com.adorsys.webank.serviceimpl;
//
//import com.adorsys.webank.domain.PersonalInfoEntity;
//import com.adorsys.webank.domain.PersonalInfoStatus;
//import com.adorsys.webank.domain.UserDocumentsEntity;
//import com.adorsys.webank.dto.*;
//import com.adorsys.webank.exceptions.FailedToSendOTPException;
//import com.adorsys.webank.repository.PersonalInfoRepository;
//import com.adorsys.webank.repository.UserDocumentsRepository;
//import jakarta.persistence.EntityNotFoundException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class KycServiceImplTest {
//
//    @Mock
//    private PersonalInfoRepository personalInfoRepository;
//
//    @Mock
//    private UserDocumentsRepository userDocumentsRepository;
//
//    @InjectMocks
//    private KycServiceImpl kycService;
//
//    private static final String TEST_ACCOUNT_ID = "test-account-id";
//    private static final String TEST_ID_NUMBER = "test-id-number";
//    private static final String TEST_EXPIRY_DATE = "2025-12-31";
//    private static final String TEST_LOCATION = "test-location";
//    private static final String TEST_EMAIL = "test@example.com";
//    private static final String TEST_FRONT_ID = "front-id-base64";
//    private static final String TEST_BACK_ID = "back-id-base64";
//    private static final String TEST_SELFIE_ID = "selfie-id-base64";
//    private static final String TEST_TAX_ID = "tax-id-base64";
//
//    @BeforeEach
//    void setUp() {
//        // Reset mocks before each test
//        reset(personalInfoRepository, userDocumentsRepository);
//    }
//
//    @Test
//    void sendKycDocument_Success() {
//        // Given
//        KycDocumentRequest request = new KycDocumentRequest(
//            TEST_FRONT_ID,
//            TEST_BACK_ID,
//            TEST_TAX_ID,
//            TEST_SELFIE_ID,
//            TEST_ACCOUNT_ID
//        );
//
//        when(userDocumentsRepository.save(any(UserDocumentsEntity.class)))
//            .thenReturn(new UserDocumentsEntity());
//
//        // When
//        String result = kycService.sendKycDocument(TEST_ACCOUNT_ID, request);
//
//        // Then
//        assertEquals("KYC Document sent successfully and saved", result);
//        verify(userDocumentsRepository).save(any(UserDocumentsEntity.class));
//    }
//
//    @Test
//    void sendKycDocument_NullRequest_ThrowsException() {
//        // When & Then
//        assertThrows(IllegalArgumentException.class, () -> {
//            kycService.sendKycDocument(TEST_ACCOUNT_ID, null);
//        });
//    }
//
//    @Test
//    void sendKycInfo_Success() {
//        // Given
//        KycInfoRequest request = new KycInfoRequest(
//            TEST_ID_NUMBER,
//            TEST_EXPIRY_DATE,
//            TEST_ACCOUNT_ID
//        );
//
//        when(personalInfoRepository.save(any(PersonalInfoEntity.class)))
//            .thenReturn(new PersonalInfoEntity());
//
//        // When
//        String result = kycService.sendKycInfo(TEST_ACCOUNT_ID, request);
//
//        // Then
//        assertEquals("KYC Info sent successfully and saved.", result);
//        verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
//    }
//
//    @Test
//    void sendKycInfo_NullRequest_ThrowsException() {
//        // When & Then
//        assertThrows(IllegalArgumentException.class, () -> {
//            kycService.sendKycInfo(TEST_ACCOUNT_ID, null);
//        });
//    }
//
//    @Test
//    void sendKycLocation_Success() {
//        // Given
//        KycLocationRequest request = new KycLocationRequest(
//            TEST_LOCATION,
//            TEST_ACCOUNT_ID
//        );
//
//        PersonalInfoEntity existingInfo = new PersonalInfoEntity();
//        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
//            .thenReturn(Optional.of(existingInfo));
//        when(personalInfoRepository.save(any(PersonalInfoEntity.class)))
//            .thenReturn(existingInfo);
//
//        // When
//        String result = kycService.sendKycLocation(request);
//
//        // Then
//        assertEquals("KYC Location updated successfully.", result);
//        verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
//    }
//    @Test
//    void sendKycEmail_Success() {
//        // Given
//        KycEmailRequest request = new KycEmailRequest(
//            TEST_EMAIL,
//            TEST_ACCOUNT_ID
//        );
//
//        PersonalInfoEntity existingInfo = new PersonalInfoEntity();
//        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
//            .thenReturn(Optional.of(existingInfo));
//        when(personalInfoRepository.save(any(PersonalInfoEntity.class)))
//            .thenReturn(existingInfo);
//
//        // When
//        String result = kycService.sendKycEmail(request);
//
//        // Then
//        assertEquals("KYC Email updated successfully.", result);
//        verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
//    }
//
//
//    @Test
//    void getPersonalInfoAccountId_Success() {
//        // Given
//        PersonalInfoEntity info = new PersonalInfoEntity();
//        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
//            .thenReturn(Optional.of(info));
//
//        // When
//        Optional<PersonalInfoEntity> result = kycService.getPersonalInfoAccountId(TEST_ACCOUNT_ID);
//
//        // Then
//        assertTrue(result.isPresent());
//        assertEquals(info, result.get());
//    }
//
//    @Test
//    void getPersonalInfoAccountId_NotFound() {
//        // Given
//        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
//            .thenReturn(Optional.empty());
//
//        // When
//        Optional<PersonalInfoEntity> result = kycService.getPersonalInfoAccountId(TEST_ACCOUNT_ID);
//
//        // Then
//        assertFalse(result.isPresent());
//    }
//
//
//    @Test
//    void sendKycDocument_ValidFileSize_Success() {
//        // Given
//        // Create a base64 string that would result in a file size < 200KB
//        String validBase64String = "a".repeat(100 * 1024); // 100KB worth of base64 data
//
//        KycDocumentRequest request = new KycDocumentRequest(
//            validBase64String, // frontId
//            TEST_BACK_ID,
//            TEST_TAX_ID,
//            TEST_SELFIE_ID,
//            TEST_ACCOUNT_ID
//        );
//
//        when(userDocumentsRepository.save(any(UserDocumentsEntity.class)))
//            .thenReturn(new UserDocumentsEntity());
//
//        // When
//        String result = kycService.sendKycDocument(TEST_ACCOUNT_ID, request);
//
//        // Then
//        assertEquals("KYC Document sent successfully and saved", result);
//        verify(userDocumentsRepository).save(any(UserDocumentsEntity.class));
//    }
//
//    @Test
//    void sendKycDocument_EmptyDocument_Success() {
//        // Given
//        KycDocumentRequest request = new KycDocumentRequest(
//            "", // empty frontId
//            TEST_BACK_ID,
//            TEST_TAX_ID,
//            TEST_SELFIE_ID,
//            TEST_ACCOUNT_ID
//        );
//
//        when(userDocumentsRepository.save(any(UserDocumentsEntity.class)))
//            .thenReturn(new UserDocumentsEntity());
//
//        // When
//        String result = kycService.sendKycDocument(TEST_ACCOUNT_ID, request);
//
//        // Then
//        assertEquals("KYC Document sent successfully and saved", result);
//        verify(userDocumentsRepository).save(any(UserDocumentsEntity.class));
//    }
//}