package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.config.*;
import com.adorsys.webank.domain.*;
import com.adorsys.webank.projection.*;
import com.adorsys.webank.repository.*;
import com.nimbusds.jose.jwk.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        reset(personalInfoRepository, certGeneratorHelper);
    }

    @Test
    void getCert_whenAccountIsRejected_shouldReturnRejectionReason() {
        // Arrange
        PersonalInfoProjection rejectedProjection = mock(PersonalInfoProjection.class);
        when(rejectedProjection.getStatus()).thenReturn(PersonalInfoStatus.REJECTED);
        when(rejectedProjection.getRejectionReason()).thenReturn(TEST_REJECTION_REASON);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(rejectedProjection));

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
        PersonalInfoProjection rejectedProjection = mock(PersonalInfoProjection.class);
        when(rejectedProjection.getStatus()).thenReturn(PersonalInfoStatus.REJECTED);
        when(rejectedProjection.getRejectionReason()).thenReturn("");

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(rejectedProjection));

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
        PersonalInfoProjection approvedProjection = mock(PersonalInfoProjection.class);
        when(approvedProjection.getStatus()).thenReturn(PersonalInfoStatus.APPROVED);

        ECKey mockDevicePub = mock(ECKey.class);
        when(mockDevicePub.toJSONString()).thenReturn("{\"kty\":\"EC\"}");

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(mockDevicePub);
            when(certGeneratorHelper.generateCertificate(anyString())).thenThrow(new RuntimeException("Simulated failure"));
            when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(approvedProjection));

            // Act
            String result = kycCertService.getCert(TEST_ACCOUNT_ID);

            // Assert
            assertNotNull(result);
            assertEquals("null", result);
        }
    }
}