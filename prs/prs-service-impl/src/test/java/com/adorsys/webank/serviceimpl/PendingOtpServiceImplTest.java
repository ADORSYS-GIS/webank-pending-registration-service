package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.projection.OtpProjection;
import com.adorsys.webank.repository.OtpRequestRepository;

@ExtendWith(MockitoExtension.class)
class PendingOtpServiceImplTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;

    @InjectMocks
    private PendingOtpServiceImpl pendingOtpService;

    private static final String TEST_PHONE_1 = "+1234567890";
    private static final String TEST_PHONE_2 = "+0987654321";
    private static final String TEST_OTP_1 = "111111";
    private static final String TEST_OTP_2 = "222222";

    @BeforeEach
    void setUp() {
        // No additional setup needed
    }

    @Test
    void testFetchPendingOtpEntries_Success() {
        // Arrange
        OtpProjection otpProjection1 = mock(OtpProjection.class);
        when(otpProjection1.getPhoneNumber()).thenReturn(TEST_PHONE_1);
        when(otpProjection1.getOtpCode()).thenReturn(TEST_OTP_1);
        when(otpProjection1.getStatus()).thenReturn(OtpStatus.PENDING);

        OtpProjection otpProjection2 = mock(OtpProjection.class);
        when(otpProjection2.getPhoneNumber()).thenReturn(TEST_PHONE_2);
        when(otpProjection2.getOtpCode()).thenReturn(TEST_OTP_2);
        when(otpProjection2.getStatus()).thenReturn(OtpStatus.PENDING);

        List<OtpProjection> otpList = List.of(otpProjection1, otpProjection2);
        
        when(otpRequestRepository.findByStatus(OtpStatus.PENDING)).thenReturn(otpList);

        // Act
        List<PendingOtpDto> result = pendingOtpService.fetchPendingOtpEntries();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify first DTO
        PendingOtpDto dto1 = result.get(0);
        assertEquals(TEST_PHONE_1, dto1.getPhoneNumber());
        assertEquals(TEST_OTP_1, dto1.getOtpCode());
        assertEquals(OtpStatus.PENDING.name(), dto1.getStatus());

        // Verify second DTO
        PendingOtpDto dto2 = result.get(1);
        assertEquals(TEST_PHONE_2, dto2.getPhoneNumber());
        assertEquals(TEST_OTP_2, dto2.getOtpCode());
        assertEquals(OtpStatus.PENDING.name(), dto2.getStatus());

        verify(otpRequestRepository).findByStatus(OtpStatus.PENDING);
    }

    @Test
    void testFetchPendingOtpEntries_EmptyList() {
        // Arrange
        when(otpRequestRepository.findByStatus(OtpStatus.PENDING)).thenReturn(Collections.emptyList());

        // Act
        List<PendingOtpDto> result = pendingOtpService.fetchPendingOtpEntries();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(otpRequestRepository).findByStatus(OtpStatus.PENDING);
    }

    @Test
    void testFetchPendingOtpEntries_NoPendingEntries() {
        // Arrange
        when(otpRequestRepository.findByStatus(OtpStatus.PENDING)).thenReturn(Collections.emptyList());

        // Act
        List<PendingOtpDto> result = pendingOtpService.fetchPendingOtpEntries();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(otpRequestRepository).findByStatus(OtpStatus.PENDING);
    }
}
