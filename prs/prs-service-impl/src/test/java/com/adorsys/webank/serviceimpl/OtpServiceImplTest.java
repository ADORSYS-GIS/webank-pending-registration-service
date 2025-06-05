package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.exceptions.OtpValidationException;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.security.HashHelper;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OtpServiceImplTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;

    @Mock
    private HashHelper hashHelper;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private Argon2PasswordEncoder passwordEncoder;
    
    private OtpServiceImpl otpService;

    private ECKey devicePublicKey;
    private String phoneNumber = "+1234567890";

    @BeforeEach
    void setUp() throws Exception {
        // Generate test EC keys for server and device
        devicePublicKey = new ECKeyGenerator(Curve.P_256).generate().toPublicJWK();
        
        // Create service with mocked dependencies
        otpService = spy(new OtpServiceImpl(otpRequestRepository, hashHelper, objectMapper, passwordEncoder));
        

        
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
        // Create an OtpEntity instead of OtpProjection since we're now using findEntityByPublicKeyHash
        OtpEntity expiredEntity = new OtpEntity();
        expiredEntity.setPhoneNumber(phoneNumber);
        expiredEntity.setPublicKeyHash("test-public-key-hash");
        expiredEntity.setOtpHash("test-hash");
        expiredEntity.setOtpCode("12345");
        expiredEntity.setStatus(OtpStatus.PENDING);
        expiredEntity.setCreatedAt(LocalDateTime.now().minusMinutes(6)); // Created 6 minutes ago

        when(otpRequestRepository.findEntityByPublicKeyHash(any())).thenReturn(Optional.of(expiredEntity));
        // No need to configure verification behavior as OTP should be expired before verification

        OtpValidationException exception = assertThrows(OtpValidationException.class, 
            () -> otpService.validateOtp(phoneNumber, devicePublicKey, "12345"));

        assertEquals("OTP expired. Request a new one.", exception.getMessage());
        verify(otpRequestRepository).save(any(OtpEntity.class));
    }

    @Test
    void validateOtpInvalidOtpReturnsInvalidMessage() {
        // Create an OtpEntity instead of OtpProjection
        OtpEntity otpEntity = new OtpEntity();
        otpEntity.setPhoneNumber(phoneNumber);
        otpEntity.setPublicKeyHash("test-public-key-hash");
        otpEntity.setOtpHash("test-hash");
        otpEntity.setOtpCode("12345");
        otpEntity.setStatus(OtpStatus.PENDING);
        otpEntity.setCreatedAt(LocalDateTime.now()); // Fresh OTP, not expired

        when(otpRequestRepository.findEntityByPublicKeyHash(any())).thenReturn(Optional.of(otpEntity));
        // Since we can't mock the internal passwordEncoder, we'll mock the repository response
        // and use a spy to override the behavior we need to test

        OtpValidationException exception = assertThrows(OtpValidationException.class, 
            () -> otpService.validateOtp(phoneNumber, devicePublicKey, "12345"));

        assertEquals("Invalid OTP", exception.getMessage());
        verify(otpRequestRepository).save(any(OtpEntity.class));
    }


}