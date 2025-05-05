package com.adorsys.webank;

import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.PendingOtpServiceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingOtpListRestServerTest {

    private static final String VALID_AUTH_HEADER = "Bearer valid.jwt.token";
    private static final String INVALID_AUTH_HEADER = "InvalidHeader";
    private static final String PHONE_NUMBER = "1234567890";
    private static final String OTP_CODE = "12345";
    private static final String STATUS = "PENDING";

    @Mock
    private PendingOtpServiceApi pendingOtpServiceApi;

    @InjectMocks
    private PendingOtpListRestServer pendingOtpListRestServer;

    private List<PendingOtpDto> expectedOtpList;

    @BeforeEach
    void setUp() {
        PendingOtpDto otp1 = new PendingOtpDto(PHONE_NUMBER, OTP_CODE, STATUS);
        PendingOtpDto otp2 = new PendingOtpDto("0987654321", "54321", "PENDING");
        expectedOtpList = Arrays.asList(otp1, otp2);
    }

    @Test
    void getPendingOtps_WithValidRequest_ShouldReturnList() {
        // Given
        try (MockedStatic<JwtValidator> jwtValidator = mockStatic(JwtValidator.class)) {
            jwtValidator.when(() -> JwtValidator.validateAndExtract(anyString()))
                .thenReturn(null);

            when(pendingOtpServiceApi.fetchPendingOtpEntries()).thenReturn(expectedOtpList);

            // When
            List<PendingOtpDto> response = pendingOtpListRestServer.getPendingOtps(VALID_AUTH_HEADER);

            // Then
            assertThat(response).hasSize(2);
            assertThat(response).isEqualTo(expectedOtpList);
        }
    }

    @Test
    void getPendingOtps_WithInvalidAuthHeader_ShouldThrowException() {
        // When/Then
        assertThrows(
            IllegalArgumentException.class,
            () -> pendingOtpListRestServer.getPendingOtps(INVALID_AUTH_HEADER)
        );
    }
} 