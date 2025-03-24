package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.repository.OtpRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingOtpServiceImplTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;

    @InjectMocks
    private PendingOtpServiceImpl pendingOtpServiceImpl;

    private OtpEntity otpEntity1;
    private OtpEntity otpEntity2;

    @BeforeEach
    void setUp() {
        // Create mocked OtpEntity objects with desired behavior.
        otpEntity1 = mock(OtpEntity.class);
        when(otpEntity1.getPhoneNumber()).thenReturn("1234567890");
        when(otpEntity1.getOtpCode()).thenReturn("111111");
        when(otpEntity1.getStatus()).thenReturn(OtpStatus.PENDING);

        otpEntity2 = mock(OtpEntity.class);
        when(otpEntity2.getPhoneNumber()).thenReturn("0987654321");
        when(otpEntity2.getOtpCode()).thenReturn("222222");
        when(otpEntity2.getStatus()).thenReturn(OtpStatus.PENDING);
    }

    @Test
    void testGetPendingOtps() {
        // Stub the repository to return the mocked OtpEntity objects.
        List<OtpEntity> otpList = List.of(otpEntity1, otpEntity2);
        when(otpRequestRepository.findByStatus(OtpStatus.PENDING)).thenReturn(otpList);

        // Act: call the service method.
        List<PendingOtpDto> result = pendingOtpServiceImpl.getPendingOtps();

        // Assert: ensure proper mapping from OtpEntity to PendingOtpDto.
        assertEquals(2, result.size());

        PendingOtpDto dto1 = result.get(0);
        PendingOtpDto dto2 = result.get(1);

        assertEquals("1234567890", dto1.getPhoneNumber());
        assertEquals("111111", dto1.getOtpCode());
        assertEquals("PENDING", dto1.getStatus());

        assertEquals("0987654321", dto2.getPhoneNumber());
        assertEquals("222222", dto2.getOtpCode());
        assertEquals("PENDING", dto2.getStatus());
    }
}
