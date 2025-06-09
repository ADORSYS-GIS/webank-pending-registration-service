package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.domain.UserDocumentsEntity;
import com.adorsys.webank.domain.UserDocumentsStatus;
import com.adorsys.webank.dto.KycDocumentRequest;
import com.adorsys.webank.dto.KycEmailRequest;
import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.dto.KycLocationRequest;
import com.adorsys.webank.dto.UserInfoResponse;
import com.adorsys.webank.exceptions.KycProcessingException;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.projection.UserDocumentsProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.repository.UserDocumentsRepository;

import jakarta.persistence.EntityNotFoundException;

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
   private static final String TEST_ID_TYPE = "PASSPORT";

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
       String result = kycService.sendKycDocument(TEST_ACCOUNT_ID, request);

       // Then
       assertEquals("KYC Document sent successfully and saved", result);
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
   void sendKycDocument_ProcessingError_ThrowsException() {
       // Given
       KycDocumentRequest request = new KycDocumentRequest(
           TEST_FRONT_ID,
           TEST_BACK_ID,
           TEST_TAX_ID,
           TEST_SELFIE_ID,
           TEST_ACCOUNT_ID
       );

       when(userDocumentsRepository.save(any(UserDocumentsEntity.class)))
           .thenThrow(new RuntimeException("Database error"));

       // When & Then
       assertThrows(KycProcessingException.class, () -> {
           kycService.sendKycDocument(TEST_ACCOUNT_ID, request);
       });
   }

   @Test
   void sendKycInfo_Success() {
       // Given
       KycInfoRequest request = new KycInfoRequest(
           TEST_ID_NUMBER,
           TEST_EXPIRY_DATE,
           TEST_ACCOUNT_ID,
           TEST_ID_TYPE
       );

       when(personalInfoRepository.save(any(PersonalInfoEntity.class)))
           .thenReturn(new PersonalInfoEntity());

       // When
       String result = kycService.sendKycInfo(TEST_ACCOUNT_ID, request);

       // Then
       assertEquals("KYC Info sent successfully and saved.", result);
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
   void sendKycInfo_ProcessingError_ThrowsException() {
       // Given
       KycInfoRequest request = new KycInfoRequest(
           TEST_ID_NUMBER,
           TEST_EXPIRY_DATE,
           TEST_ACCOUNT_ID,
           TEST_ID_TYPE
       );

       when(personalInfoRepository.save(any(PersonalInfoEntity.class)))
           .thenThrow(new RuntimeException("Database error"));

       // When & Then
       assertThrows(KycProcessingException.class, () -> {
           kycService.sendKycInfo(TEST_ACCOUNT_ID, request);
       });
   }

   @Test
   void sendKycLocation_Success() {
       // Given
       KycLocationRequest request = new KycLocationRequest(
           TEST_LOCATION,
           TEST_ACCOUNT_ID
       );

       PersonalInfoProjection existingInfo = mock(PersonalInfoProjection.class);
       when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
           .thenReturn(Optional.of(existingInfo));
       when(personalInfoRepository.save(any(PersonalInfoEntity.class)))
           .thenReturn(new PersonalInfoEntity());

       // When
       String result = kycService.sendKycLocation(request);

       // Then
       assertEquals("KYC Location updated successfully.", result);
       verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
   }

   @Test
   void sendKycLocation_NullRequest_ThrowsException() {
       // When & Then
       assertThrows(IllegalArgumentException.class, () -> {
           kycService.sendKycLocation(null);
       });
   }

   @Test
   void sendKycLocation_NoRecordFound_ThrowsException() {
       // Given
       KycLocationRequest request = new KycLocationRequest(
           TEST_LOCATION,
           TEST_ACCOUNT_ID
       );

       when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
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

       PersonalInfoProjection existingInfo = mock(PersonalInfoProjection.class);
       when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
           .thenReturn(Optional.of(existingInfo));
       when(personalInfoRepository.save(any(PersonalInfoEntity.class)))
           .thenReturn(new PersonalInfoEntity());

       // When
       String result = kycService.sendKycEmail(request);

       // Then
       assertEquals("KYC Email updated successfully.", result);
       verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
   }

   @Test
   void sendKycEmail_NullRequest_ThrowsException() {
       // When & Then
       assertThrows(IllegalArgumentException.class, () -> {
           kycService.sendKycEmail(null);
       });
   }

   @Test
   void sendKycEmail_NoRecordFound_ThrowsException() {
       // Given
       KycEmailRequest request = new KycEmailRequest(
           TEST_EMAIL,
           TEST_ACCOUNT_ID
       );

       when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID))
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
   void getPendingKycRecords_Success() {
       // Given
       PersonalInfoProjection pendingInfo = mock(PersonalInfoProjection.class);
       when(pendingInfo.getAccountId()).thenReturn(TEST_ACCOUNT_ID);
       when(pendingInfo.getStatus()).thenReturn(PersonalInfoStatus.PENDING);

       UserDocumentsProjection pendingDoc = mock(UserDocumentsProjection.class);
       when(pendingDoc.getStatus()).thenReturn(UserDocumentsStatus.PENDING);

       when(personalInfoRepository.findByStatus(PersonalInfoStatus.PENDING))
           .thenReturn(Arrays.asList(pendingInfo));
       when(userDocumentsRepository.findByAccountId(TEST_ACCOUNT_ID))
           .thenReturn(Optional.of(pendingDoc));

       // When
       List<UserInfoResponse> result = kycService.getPendingKycRecords();

       // Then
       assertEquals(1, result.size());
       verify(personalInfoRepository).findByStatus(PersonalInfoStatus.PENDING);
       verify(userDocumentsRepository).findByAccountId(TEST_ACCOUNT_ID);
   }

   @Test
   void findByDocumentUniqueId_Success() {
       // Given
       PersonalInfoProjection info = mock(PersonalInfoProjection.class);
       when(info.getAccountId()).thenReturn(TEST_ACCOUNT_ID);

       UserDocumentsProjection doc = mock(UserDocumentsProjection.class);

       when(personalInfoRepository.findByDocumentUniqueId(TEST_ID_NUMBER))
           .thenReturn(Arrays.asList(info));
       when(userDocumentsRepository.findByAccountId(TEST_ACCOUNT_ID))
           .thenReturn(Optional.of(doc));

       // When
       List<UserInfoResponse> result = kycService.findByDocumentUniqueId(TEST_ID_NUMBER);

       // Then
       assertEquals(1, result.size());
       verify(personalInfoRepository).findByDocumentUniqueId(TEST_ID_NUMBER);
       verify(userDocumentsRepository).findByAccountId(TEST_ACCOUNT_ID);
   }
}