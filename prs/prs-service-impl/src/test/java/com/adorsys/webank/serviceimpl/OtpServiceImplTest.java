package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class OtpServiceImplTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private OtpServiceImpl otpService;

    private ECKey devicePublicKey;
    private String phoneNumber = "+1234567890";

    @BeforeEach
    void setUp() throws Exception {
        // Generate test EC keys for server and device
        devicePublicKey = new ECKeyGenerator(Curve.P_256)
            .generate()
            .toPublicJWK();

        // Create OtpServiceImpl with mocked dependencies
        otpService = new OtpServiceImpl(otpRequestRepository, passwordEncoder);

        // Setup spy for generateOtp method
        otpService = spy(otpService);
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

        // Mock repository methods
        when(
            otpRequestRepository.updateOtpByPublicKeyHash(
                anyString(),
                anyString(),
                any(),
                any()
            )
        ).thenReturn(0);

        // Mock password encoder for this specific test
        when(passwordEncoder.encode(anyString())).thenReturn(
            "{argon2}encoded-hash"
        );

        String otpHash = otpService.sendOtp(devicePublicKey, phoneNumber);

        // Verify repository save
        ArgumentCaptor<OtpEntity> captor = ArgumentCaptor.forClass(
            OtpEntity.class
        );
        verify(otpRequestRepository).save(captor.capture());

        OtpEntity savedRequest = captor.getValue();
        assertEquals(phoneNumber, savedRequest.getPhoneNumber());
        assertEquals("12345", savedRequest.getOtpCode());
        assertEquals(OtpStatus.PENDING, savedRequest.getStatus());
        assertNotNull(savedRequest.getPublicKeyHash());
        assertEquals("{argon2}encoded-hash", savedRequest.getOtpHash());
        assertEquals("{argon2}encoded-hash", otpHash);
    }

    @Test
    void sendOtp_InvalidPhoneNumber_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            otpService.sendOtp(devicePublicKey, "invalid")
        );
    }

    @Test
    void validateOtp_ExpiredOtp_ReturnsExpiredMessage() {
        OtpEntity expiredRequest = createTestOtpRequest("12345", 6); // Created 6 minutes ago

        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(
            Optional.of(expiredRequest)
        );

        String result = otpService.validateOtp(
            phoneNumber,
            devicePublicKey,
            "12345"
        );

        assertEquals("OTP expired. Request a new one.", result);
        assertEquals(OtpStatus.INCOMPLETE, expiredRequest.getStatus());
    }

    @Test
    void validateOtp_InvalidOtp_ReturnsInvalidMessage() {
        OtpEntity request = createTestOtpRequest("12345", 0);

        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(
            Optional.of(request)
        );
        
        // Mock password encoder for this specific test
        when(passwordEncoder.matches(anyString(), eq("test-hash"))).thenReturn(false);

        String result = otpService.validateOtp(
            phoneNumber,
            devicePublicKey,
            "12345"
        );

        assertEquals("Invalid OTP", result);
        assertEquals(OtpStatus.INCOMPLETE, request.getStatus());
    }

    @Test
    void validateOtp_ValidOtp_ReturnsSuccessMessage() {
        OtpEntity request = createTestOtpRequest("12345", 0);
        request.setOtpHash("{argon2}encoded-hash");

        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(
            Optional.of(request)
        );
        
        // Mock password encoder for this specific test
        when(passwordEncoder.matches(anyString(), eq("{argon2}encoded-hash"))).thenReturn(true);

        String result = otpService.validateOtp(
            phoneNumber,
            devicePublicKey,
            "12345"
        );

        assertEquals("Otp Validated Successfully", result);
        assertEquals(OtpStatus.COMPLETE, request.getStatus());
    }

    private OtpEntity createTestOtpRequest(String otpCode, int minutesAgo) {
        return OtpEntity.builder()
            .phoneNumber(phoneNumber)
            .publicKeyHash("test-public-key-hash")
            .otpHash("test-hash")
            .otpCode(otpCode)
            .status(OtpStatus.PENDING)
            .createdAt(LocalDateTime.now().minusMinutes(minutesAgo))
            .build();
    }
}
