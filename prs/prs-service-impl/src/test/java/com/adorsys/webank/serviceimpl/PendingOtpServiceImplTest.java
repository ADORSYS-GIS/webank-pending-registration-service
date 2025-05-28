package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        OtpEntity otpEntity1 = createOtpEntity(TEST_PHONE_1, TEST_OTP_1, OtpStatus.PENDING);
        OtpEntity otpEntity2 = createOtpEntity(TEST_PHONE_2, TEST_OTP_2, OtpStatus.PENDING);
        List<OtpEntity> otpList = List.of(otpEntity1, otpEntity2);
        
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

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            pendingOtpService.fetchPendingOtpEntries()
        );
        assertEquals("No pending OTP entries found", exception.getMessage());
        verify(otpRequestRepository).findByStatus(OtpStatus.PENDING);
    }

    @Test
    void testFetchPendingOtpEntries_NoPendingEntries() {
        // Arrange
        OtpEntity otpEntity1 = createOtpEntity(TEST_PHONE_1, TEST_OTP_1, OtpStatus.COMPLETE);
        OtpEntity otpEntity2 = createOtpEntity(TEST_PHONE_2, TEST_OTP_2, OtpStatus.INCOMPLETE);
        List<OtpEntity> otpList = List.of(otpEntity1, otpEntity2);
        
        when(otpRequestRepository.findByStatus(OtpStatus.PENDING)).thenReturn(Collections.emptyList());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            pendingOtpService.fetchPendingOtpEntries()
        );
        assertEquals("No pending OTP entries found", exception.getMessage());
        verify(otpRequestRepository).findByStatus(OtpStatus.PENDING);
    }

    private OtpEntity createOtpEntity(String phoneNumber, String otpCode, OtpStatus status) {
        OtpEntity entity = new OtpEntity();
        entity.setPhoneNumber(phoneNumber);
        entity.setOtpCode(otpCode);
        entity.setStatus(status);
        return entity;
    }
}
