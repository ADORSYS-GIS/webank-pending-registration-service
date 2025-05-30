package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import com.adorsys.webank.config.SecurityUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;
import com.adorsys.webank.config.CertGeneratorHelper;

@ExtendWith(MockitoExtension.class)
class KycCertServiceImplTest {

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @Mock
    private CertGeneratorHelper certGeneratorHelper;

    @InjectMocks
    private KycCertServiceImpl kycCertService;

    private static final String TEST_ACCOUNT_ID = "testAccountId";
    private static final String TEST_REJECTION_REASON = "Test rejection reason";

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(personalInfoRepository, certGeneratorHelper);
    }

    @Test
    void getCert_whenAccountIsRejected_shouldReturnRejectionReason() {
        // Arrange
        PersonalInfoEntity rejectedEntity = PersonalInfoEntity.builder()
                .accountId(TEST_ACCOUNT_ID)
                .status(PersonalInfoStatus.REJECTED)
                .rejectionReason(TEST_REJECTION_REASON)
                .build();

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(rejectedEntity));

        // Act
        String result = kycCertService.getCert(TEST_ACCOUNT_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("REJECTED: "));
        assertTrue(result.contains(TEST_REJECTION_REASON));
    }

    @Test
    void getCert_whenAccountIsRejectedWithoutReason_shouldReturnDefaultMessage() {
        // Arrange
        PersonalInfoEntity rejectedEntity = PersonalInfoEntity.builder()
                .accountId(TEST_ACCOUNT_ID)
                .status(PersonalInfoStatus.REJECTED)
                .rejectionReason("")
                .build();

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(rejectedEntity));

        // Act
        String result = kycCertService.getCert(TEST_ACCOUNT_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("REJECTED: "));
        assertTrue(result.contains("Please check your documents"));
    }

    @Test
    void getCert_whenAccountNotFound_shouldReturnNull() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // Act
        String result = kycCertService.getCert(TEST_ACCOUNT_ID);

        // Assert
        assertNull(result);
    }

    @Test
    void getCert_whenCertificateGenerationFails_shouldReturnNull() {
        // Arrange
        PersonalInfoEntity approvedEntity = PersonalInfoEntity.builder()
                .accountId(TEST_ACCOUNT_ID)
                .status(PersonalInfoStatus.APPROVED)
                .build();

        ECKey mockDevicePub = mock(ECKey.class);
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext)
                .thenReturn(mockDevicePub);

            when(certGeneratorHelper.generateCertificate(anyString())).thenThrow(new RuntimeException("Test exception"));
            when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(approvedEntity));

            // Act
            String result = kycCertService.getCert(TEST_ACCOUNT_ID);

            // Assert
            assertNotNull(result);
            assertEquals("null", result);
        }
    }
}