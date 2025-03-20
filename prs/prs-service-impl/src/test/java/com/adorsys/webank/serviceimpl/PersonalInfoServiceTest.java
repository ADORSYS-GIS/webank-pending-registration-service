//package com.adorsys.webank.serviceimpl;
//
//import com.adorsys.webank.domain.PersonalInfoEntity;
//import com.adorsys.webank.domain.PersonalInfoStatus;
//import com.adorsys.webank.repository.PersonalInfoRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class PersonalInfoServiceTest {
//
//    @Mock
//    private PersonalInfoRepository repository;
//
//    @InjectMocks
//    private PersonalInfoServiceImpl service;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void savePersonalInfo_shouldStoreInfoSuccessfully() {
//        // Arrange
//        PersonalInfoEntity entity = new PersonalInfoEntity();
//        entity.setPublicKeyHash("test-public-key");
//        when(repository.save(any(PersonalInfoEntity.class))).thenReturn(entity);
//
//        // Act
//        service.savePersonalInfo(entity);
//
//        // Assert
//        verify(repository, times(1)).save(entity);
//    }
//
//    @Test
//    void getPersonalInfoByPublicKey_shouldReturnInfoIfExists() {
//        // Arrange
//        String publicKeyHash = "test-public-key";
//        PersonalInfoEntity entity = new PersonalInfoEntity();
//        entity.setPublicKeyHash(publicKeyHash);
//
//        when(repository.findByPublicKeyHash(publicKeyHash)).thenReturn(Optional.of(entity));
//
//        // Act
//        Optional<PersonalInfoEntity> result = service.getPersonalInfoByPublicKey(publicKeyHash);
//
//        // Assert
//        assertTrue(result.isPresent());
//        assertEquals(publicKeyHash, result.get().getPublicKeyHash());
//    }
//
//    @Test
//    void getPersonalInfoByPublicKey_shouldReturnEmptyIfNotExists() {
//        // Arrange
//        String publicKeyHash = "non-existing-key";
//        when(repository.findByPublicKeyHash(publicKeyHash)).thenReturn(Optional.empty());
//
//        // Act
//        Optional<PersonalInfoEntity> result = service.getPersonalInfoByPublicKey(publicKeyHash);
//
//        // Assert
//        assertFalse(result.isPresent());
//    }
//
//    @Test
//    void getPersonalInfoByStatus_shouldReturnListOfMatchingRecords() {
//        // Arrange
//        PersonalInfoStatus status = PersonalInfoStatus.PENDING;
//        PersonalInfoEntity entity1 = new PersonalInfoEntity();
//        PersonalInfoEntity entity2 = new PersonalInfoEntity();
//        List<PersonalInfoEntity> entities = List.of(entity1, entity2);
//
//        when(repository.findByStatus(status)).thenReturn(entities);
//
//        // Act
//        List<PersonalInfoEntity> result = service.getPersonalInfoByStatus(status);
//
//        // Assert
//        assertEquals(2, result.size());
//        verify(repository, times(1)).findByStatus(status);
//    }
//}
