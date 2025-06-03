package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.projection.OtpProjection;
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

    private OtpProjection otpProjection1;
    private OtpProjection otpProjection2;

    @BeforeEach
    void setUp() {
        // Create mocked OtpProjection objects with desired behavior
        otpProjection1 = mock(OtpProjection.class);
        when(otpProjection1.getPhoneNumber()).thenReturn("1234567890");
        when(otpProjection1.getOtpCode()).thenReturn("111111");
        when(otpProjection1.getStatus()).thenReturn(OtpStatus.PENDING);

        otpProjection2 = mock(OtpProjection.class);
        when(otpProjection2.getPhoneNumber()).thenReturn("0987654321");
        when(otpProjection2.getOtpCode()).thenReturn("222222");
        when(otpProjection2.getStatus()).thenReturn(OtpStatus.PENDING);
    }

    @Test
    void fetchPendingOtpEntries_ReturnsListOfPendingOtpDtos() {
        // Arrange
        when(otpRequestRepository.findByStatus(OtpStatus.PENDING))
                .thenReturn(List.of(otpProjection1, otpProjection2));

        // Act
        List<PendingOtpDto> result = pendingOtpServiceImpl.fetchPendingOtpEntries();

        // Assert
        assertEquals(2, result.size());
        assertEquals("1234567890", result.get(0).getPhoneNumber());
        assertEquals("111111", result.get(0).getOtpCode());
        assertEquals("PENDING", result.get(0).getStatus());
        assertEquals("0987654321", result.get(1).getPhoneNumber());
        assertEquals("222222", result.get(1).getOtpCode());
        assertEquals("PENDING", result.get(1).getStatus());
    }
}
