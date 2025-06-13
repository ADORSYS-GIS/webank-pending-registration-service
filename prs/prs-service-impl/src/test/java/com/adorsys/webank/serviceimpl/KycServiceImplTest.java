package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.UserDocumentsEntity;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.dto.response.*;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.repository.UserDocumentsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceImplTest {

   @Mock
   private PersonalInfoRepository personalInfoRepository;

   @Mock
   private UserDocumentsRepository userDocumentsRepository;

   @InjectMocks
   private KycServiceImpl kycService;

   private static final String TEST_ACCOUNT_ID = "test-account-id";
   private static final String TEST_ID_NUMBER = "test-id-number";
   private static final String TEST_EXPIRY_DATE = "2025-12-31";
   private static final String TEST_LOCATION = "test-location";
   private static final String TEST_EMAIL = "test@example.com";
   private static final String TEST_FRONT_ID = "front-id-base64";
   private static final String TEST_BACK_ID = "back-id-base64";
   private static final String TEST_SELFIE_ID = "selfie-id-base64";
   private static final String TEST_TAX_ID = "tax-id-base64";

   @BeforeEach
   void setUp() {
       // Reset mocks before each test
       reset(personalInfoRepository, userDocumentsRepository);
   }

   @Test
   void sendKycDocument_Success() {
       // Given
       KycDocumentRequest request = new KycDocumentRequest(
           TEST_FRONT_ID,
           TEST_BACK_ID,
           TEST_TAX_ID,
           TEST_SELFIE_ID,
           TEST_ACCOUNT_ID
       );

       when(userDocumentsRepository.save(any(UserDocumentsEntity.class)))
           .thenReturn(new UserDocumentsEntity());

       // When
       KycDocumentResponse response = kycService.sendKycDocument(TEST_ACCOUNT_ID, request);

       // Then
       assertEquals("KYC Document sent successfully and saved", response.getMessage());
       verify(userDocumentsRepository).save(any(UserDocumentsEntity.class));
   }

   @Test
   void sendKycDocument_NullRequest_ThrowsException() {
       // When & Then
       assertThrows(IllegalArgumentException.class, () -> {
           kycService.sendKycDocument(TEST_ACCOUNT_ID, null);
       });
   }

   @Test
   void sendKycInfo_Success() {
       // Given
       KycInfoRequest request = new KycInfoRequest();
       request.setIdNumber(TEST_ID_NUMBER);
       request.setExpiryDate(TEST_EXPIRY_DATE);
       request.setAccountId(TEST_ACCOUNT_ID);
       request.setRejectionReason(null);

       when(personalInfoRepository.save(any(PersonalInfoEntity.class)))
           .thenReturn(new PersonalInfoEntity());

       // When
       KycInfoResponse response = kycService.sendKycInfo(TEST_ACCOUNT_ID, request);

       // Then
       assertEquals("KYC Info submitted successfully", response.getMessage());
       verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
   }

   @Test
   void sendKycInfo_NullRequest_ThrowsException() {
       // When & Then
       assertThrows(IllegalArgumentException.class, () -> {
           kycService.sendKycInfo(TEST_ACCOUNT_ID, null);
       });
   }

   @Test
   void sendKycLocation_Success() {
       // Given
       KycLocationRequest request = new KycLocationRequest(
           TEST_LOCATION,
           TEST_ACCOUNT_ID
       );

       PersonalInfoEntity existingInfo = new PersonalInfoEntity();
       when(personalInfoRepository.findById(TEST_ACCOUNT_ID))
           .thenReturn(Optional.of(existingInfo));
       when(personalInfoRepository.save(any(PersonalInfoEntity.class)))
           .thenReturn(existingInfo);

       // When
       KycLocationResponse response = kycService.sendKycLocation(request);

       // Then
       assertEquals("KYC Location submitted successfully", response.getMessage());
       verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
   }

   @Test
   void sendKycLocation_NotFound() {
       // Given
       KycLocationRequest request = new KycLocationRequest(
           TEST_LOCATION,
           TEST_ACCOUNT_ID
       );

       when(personalInfoRepository.findById(TEST_ACCOUNT_ID))
           .thenReturn(Optional.empty());

       // When & Then
       assertThrows(EntityNotFoundException.class, () -> {
           kycService.sendKycLocation(request);
       });
   }

   @Test
   void sendKycEmail_Success() {
       // Given
       KycEmailRequest request = new KycEmailRequest(
           TEST_EMAIL,
           TEST_ACCOUNT_ID
       );

       PersonalInfoEntity existingInfo = new PersonalInfoEntity();
       when(personalInfoRepository.findById(TEST_ACCOUNT_ID))
           .thenReturn(Optional.of(existingInfo));
       when(personalInfoRepository.save(any(PersonalInfoEntity.class)))
           .thenReturn(existingInfo);

       // When
       KycEmailResponse response = kycService.sendKycEmail(request);

       // Then
       assertEquals("KYC Email submitted successfully", response.getMessage());
       verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
   }

   @Test
   void sendKycEmail_NotFound() {
       // Given
       KycEmailRequest request = new KycEmailRequest(
           TEST_EMAIL,
           TEST_ACCOUNT_ID
       );

       when(personalInfoRepository.findById(TEST_ACCOUNT_ID))
           .thenReturn(Optional.empty());

       // When & Then
       assertThrows(EntityNotFoundException.class, () -> {
           kycService.sendKycEmail(request);
       });
   }

   @Test
   void getPersonalInfoAccountId_Success() {
       // Given
       PersonalInfoProjection info = mock(PersonalInfoProjection.class);
       when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
           .thenReturn(Optional.of(info));

       // When
       Optional<PersonalInfoProjection> result = kycService.getPersonalInfoAccountId(TEST_ACCOUNT_ID);

       // Then
       assertTrue(result.isPresent());
       assertEquals(info, result.get());
   }

   @Test
   void getPersonalInfoAccountId_NotFound() {
       // Given
       when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
           .thenReturn(Optional.empty());

       // When
       Optional<PersonalInfoProjection> result = kycService.getPersonalInfoAccountId(TEST_ACCOUNT_ID);

       // Then
       assertFalse(result.isPresent());
   }

   @Test
   void sendKycDocument_ValidFileSize_Success() {
       // Given
       // Create a base64 string that would result in a file size < 200KB
       String validBase64String = "a".repeat(100 * 1024); // 100KB worth of base64 data

       KycDocumentRequest request = new KycDocumentRequest(
           validBase64String, // frontId
           TEST_BACK_ID,
           TEST_TAX_ID,
           TEST_SELFIE_ID,
           TEST_ACCOUNT_ID
       );

       when(userDocumentsRepository.save(any(UserDocumentsEntity.class)))
           .thenReturn(new UserDocumentsEntity());

       // When
       KycDocumentResponse response = kycService.sendKycDocument(TEST_ACCOUNT_ID, request);

       // Then
       assertEquals("KYC Document sent successfully and saved", response.getMessage());
       verify(userDocumentsRepository).save(any(UserDocumentsEntity.class));
   }

   @Test
   void sendKycDocument_EmptyDocument_Success() {
       // Given
       KycDocumentRequest request = new KycDocumentRequest(
           "", // empty frontId
           TEST_BACK_ID,
           TEST_TAX_ID,
           TEST_SELFIE_ID,
           TEST_ACCOUNT_ID
       );

       when(userDocumentsRepository.save(any(UserDocumentsEntity.class)))
           .thenReturn(new UserDocumentsEntity());

       // When
       KycDocumentResponse response = kycService.sendKycDocument(TEST_ACCOUNT_ID, request);

       // Then
       assertEquals("KYC Document sent successfully and saved", response.getMessage());
       verify(userDocumentsRepository).save(any(UserDocumentsEntity.class));
   }
}