package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.projection.OtpProjection;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OtpServiceImplTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;

    @Spy
    @InjectMocks
    private OtpServiceImpl otpService;

    private ECKey devicePublicKey;
    private String phoneNumber = "+1234567890";

    @BeforeEach
    void setUp() throws Exception {
        // Generate test EC keys for server and device
        devicePublicKey = new ECKeyGenerator(Curve.P_256).generate().toPublicJWK();

        ReflectionTestUtils.setField(otpService, "salt", "test-salt");
    }

    @Test
    void generateOtp_ReturnsFiveDigitNumber() {
        String otp = otpService.generateOtp();
        assertEquals(5, otp.length());
        assertTrue(otp.matches("\\d{5}"));
    }

    @Test
    void sendOtp_ValidPhoneNumber_SavesOtpRequestAndReturnsHash() {
        // Stub generateOtp to return fixed value
        doReturn("12345").when(otpService).generateOtp();

        String otpHash = otpService.sendOtp(devicePublicKey, phoneNumber);

        // Verify repository save
        ArgumentCaptor<OtpEntity> captor = ArgumentCaptor.forClass(OtpEntity.class);
        verify(otpRequestRepository).save(captor.capture());

        OtpEntity savedRequest = captor.getValue();
        assertEquals(phoneNumber, savedRequest.getPhoneNumber());
        assertEquals("12345", savedRequest.getOtpCode());
        assertEquals(OtpStatus.PENDING, savedRequest.getStatus());
        assertNotNull(savedRequest.getPublicKeyHash());
        assertEquals(otpHash, savedRequest.getOtpHash());
    }

    @Test
    void sendOtp_InvalidPhoneNumber_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> otpService.sendOtp(devicePublicKey, "invalid"));
    }

    @Test
    void validateOtp_ExpiredOtp_ReturnsExpiredMessage() {
        OtpProjection expiredRequest = createTestOtpProjection("12345", 6); // Created 6 minutes ago

        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(expiredRequest));

        String result = otpService.validateOtp(phoneNumber, devicePublicKey, "12345");

        assertEquals("OTP expired. Request a new one.", result);
        verify(otpRequestRepository).save(any(OtpEntity.class));
    }

    @Test
    void validateOtp_InvalidOtp_ReturnsInvalidMessage() {
        OtpProjection request = createTestOtpProjection("12345", 0);

        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(request));

        String result = otpService.validateOtp(phoneNumber, devicePublicKey, "12345");

        assertEquals("Invalid OTP", result);
        verify(otpRequestRepository).save(any(OtpEntity.class));
    }

    private OtpProjection createTestOtpProjection(String otpCode, int minutesAgo) {
        OtpProjection projection = mock(OtpProjection.class);
        when(projection.getPhoneNumber()).thenReturn(phoneNumber);
        when(projection.getPublicKeyHash()).thenReturn("test-public-key-hash");
        when(projection.getOtpHash()).thenReturn("test-hash");
        when(projection.getOtpCode()).thenReturn(otpCode);
        when(projection.getStatus()).thenReturn(OtpStatus.PENDING);
        when(projection.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(minutesAgo));
        return projection;
    }
}