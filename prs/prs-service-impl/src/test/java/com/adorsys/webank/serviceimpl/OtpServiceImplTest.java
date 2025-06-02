package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.security.HashHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class OtpServiceImplTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;

    @Mock
    private HashHelper hashHelper;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private OtpServiceImpl otpService;

    private ECKey devicePublicKey;
    private String phoneNumber = "+1234567890";

    @BeforeEach
    void setUp() throws Exception {
        // Generate test EC keys for server and device
        devicePublicKey = new ECKeyGenerator(Curve.P_256).generate().toPublicJWK();
        
        // Create service with mocked dependencies
        otpService = spy(new OtpServiceImpl(otpRequestRepository, hashHelper, objectMapper));
        

        
        // Configure default behavior for hash helper in lenient mode
        lenient().when(hashHelper.calculateSHA256AsHex(anyString())).thenReturn("deterministicHashValue");
        
        // Configure default behavior for ObjectMapper in lenient mode
        try {
            lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set up mock ObjectMapper", e);
        }
    }

    @Test
    void generateOtpReturnsFiveDigitNumber() {
        String otp = otpService.generateOtp();
        assertEquals(5, otp.length());
        assertTrue(otp.matches("\\d{5}"));
    }

    @Test
    void sendOtpValidPhoneNumberSavesOtpRequestAndReturnsHash() {
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
    void sendOtpInvalidPhoneNumberThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> otpService.sendOtp(devicePublicKey, "invalid"));
    }

    @Test
    void validateOtpExpiredOtpReturnsExpiredMessage() {
        OtpEntity expiredRequest = createTestOtpRequest("12345", 6); // Created 6 minutes ago

        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(expiredRequest));
        // No need to configure verification behavior as OTP should be expired before verification

        String result = otpService.validateOtp(phoneNumber, devicePublicKey, "12345");

        assertEquals("OTP expired. Request a new one.", result);
        assertEquals(OtpStatus.INCOMPLETE, expiredRequest.getStatus());
    }

    @Test
    void validateOtpInvalidOtpReturnsInvalidMessage() {
        OtpEntity request = createTestOtpRequest("12345", 0);

        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(request));
        // Since we can't mock the internal passwordEncoder, we'll mock the repository response
        // and use a spy to override the behavior we need to test

        String result = otpService.validateOtp(phoneNumber, devicePublicKey, "12345");

        assertEquals("Invalid OTP", result);
        assertEquals(OtpStatus.INCOMPLETE, request.getStatus());
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