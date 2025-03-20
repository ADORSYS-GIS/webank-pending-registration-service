//package com.adorsys.webank.serviceimpl;
//
//import com.adorsys.webank.domain.UserDocumentsEntity;
//import com.adorsys.webank.repository.UserDocumentsRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.mock.web.MockMultipartFile;
//
//import java.io.IOException;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class UserDocumentsServiceTest {
//
//    @Mock
//    private UserDocumentsRepository repository;
//
//    @InjectMocks
//    private UserDocumentsServiceImpl service;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void saveDocuments_shouldStoreDocumentsSuccessfully() throws IOException {
//        // Arrange
//        String publicKeyHash = "test-public-key";
//        MockMultipartFile frontID = new MockMultipartFile("frontID", "front.jpg", "image/jpeg", "front".getBytes());
//        MockMultipartFile backID = new MockMultipartFile("backID", "back.jpg", "image/jpeg", "back".getBytes());
//        MockMultipartFile selfieID = new MockMultipartFile("selfieID", "selfie.jpg", "image/jpeg", "selfie".getBytes());
//        MockMultipartFile taxID = new MockMultipartFile("taxID", "tax.pdf", "application/pdf", "tax".getBytes());
//
//        UserDocumentsEntity entity = new UserDocumentsEntity();
//        entity.setPublicKeyHash(publicKeyHash);
//
//        when(repository.save(any(UserDocumentsEntity.class))).thenReturn(entity);
//
//        // Act
//        service.saveDocuments(publicKeyHash, frontID, backID, selfieID, taxID);
//
//        // Assert
//        verify(repository, times(1)).save(any(UserDocumentsEntity.class));
//    }
//
//    @Test
//    void getDocuments_shouldReturnDocumentIfExists() {
//        // Arrange
//        String publicKeyHash = "test-public-key";
//        UserDocumentsEntity entity = new UserDocumentsEntity();
//        entity.setPublicKeyHash(publicKeyHash);
//
//        when(repository.findById(publicKeyHash)).thenReturn(Optional.of(entity));
//
//        // Act
//        Optional<UserDocumentsEntity> result = service.getDocuments(publicKeyHash);
//
//        // Assert
//        assertTrue(result.isPresent());
//        assertEquals(publicKeyHash, result.get().getPublicKeyHash());
//    }
//
//    @Test
//    void getDocuments_shouldReturnEmptyIfNotExists() {
//        // Arrange
//        String publicKeyHash = "non-existing-key";
//        when(repository.findById(publicKeyHash)).thenReturn(Optional.empty());
//
//        // Act
//        Optional<UserDocumentsEntity> result = service.getDocuments(publicKeyHash);
//
//        // Assert
//        assertFalse(result.isPresent());
//    }
//}
