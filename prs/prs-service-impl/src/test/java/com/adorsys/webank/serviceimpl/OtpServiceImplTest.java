package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.error.ValidationException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;

    @Spy
    @InjectMocks
    private OtpServiceImpl otpService;

    private JWK devicePublicKey;
    private static final String TEST_PHONE_NUMBER = "+1234567890";
    private static final String TEST_SALT = "test-salt";
    private static final String TEST_OTP = "12345";

    @BeforeEach
    void setUp() throws Exception {
        devicePublicKey = new ECKeyGenerator(Curve.P_256).generate().toPublicJWK();
        ReflectionTestUtils.setField(otpService, "salt", TEST_SALT);
    }

    @Test
    void testGenerateOtp() {
        // Act
        String otp = otpService.generateOtp();

        // Assert
        assertNotNull(otp);
        assertEquals(5, otp.length());
        assertTrue(otp.matches("\\d{5}"));
    }

    @Test
    void testSendOtp_Success() {
        // Arrange
        doReturn(TEST_OTP).when(otpService).generateOtp();
        when(otpRequestRepository.updateOtpByPublicKeyHash(any(), any(), any(), any())).thenReturn(0);

        // Act
        String otpHash = otpService.sendOtp(devicePublicKey, TEST_PHONE_NUMBER);

        // Assert
        assertNotNull(otpHash);
        verify(otpRequestRepository).save(any(OtpEntity.class));
    }

    @Test
    void testSendOtp_UpdateExisting() {
        // Arrange
        doReturn(TEST_OTP).when(otpService).generateOtp();
        when(otpRequestRepository.updateOtpByPublicKeyHash(any(), any(), any(), any())).thenReturn(1);
        OtpEntity existingEntity = new OtpEntity();
        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(existingEntity));

        // Act
        String otpHash = otpService.sendOtp(devicePublicKey, TEST_PHONE_NUMBER);

        // Assert
        assertNotNull(otpHash);
        verify(otpRequestRepository).save(existingEntity);
    }

    @Test
    void testSendOtp_InvalidPhoneNumber() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            otpService.sendOtp(devicePublicKey, "invalid-phone")
        );
        verify(otpRequestRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void testSendOtp_NullPhoneNumber() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            otpService.sendOtp(devicePublicKey, null)
        );
        verify(otpRequestRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void testValidateOtp_Success() {
        // Arrange
        String otpHash = computeOtpHash(TEST_OTP);
        OtpEntity otpEntity = createTestOtpRequest(TEST_OTP, 0);
        otpEntity.setOtpHash(otpHash);
        
        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(otpEntity));

        // Act
        String result = otpService.validateOtp(TEST_PHONE_NUMBER, devicePublicKey, TEST_OTP);

        // Assert
        assertEquals("Otp Validated Successfully", result);
        assertEquals(OtpStatus.COMPLETE, otpEntity.getStatus());
        verify(otpRequestRepository).save(otpEntity);
    }

    @Test
    void testValidateOtp_NullPhoneNumber() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            otpService.validateOtp(null, devicePublicKey, TEST_OTP)
        );
        assertEquals("Phone number is required", exception.getMessage());
        verify(otpRequestRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void testValidateOtp_NullOtp() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            otpService.validateOtp(TEST_PHONE_NUMBER, devicePublicKey, null)
        );
        assertEquals("OTP is required", exception.getMessage());
        verify(otpRequestRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void testValidateOtp_NoRequestFound() {
        // Arrange
        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.empty());

        // Act
        String result = otpService.validateOtp(TEST_PHONE_NUMBER, devicePublicKey, TEST_OTP);

        // Assert
        assertEquals("No OTP request found for this public key", result);
        verify(otpRequestRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void testValidateOtp_ExpiredOtp() {
        // Arrange
        OtpEntity otpEntity = createTestOtpRequest(TEST_OTP, 6); // Created 6 minutes ago
        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(otpEntity));

        // Act
        String result = otpService.validateOtp(TEST_PHONE_NUMBER, devicePublicKey, TEST_OTP);

        // Assert
        assertEquals("OTP expired. Request a new one.", result);
        assertEquals(OtpStatus.INCOMPLETE, otpEntity.getStatus());
        verify(otpRequestRepository).save(otpEntity);
    }

    // @Test
    // void testValidateOtp_InvalidOtp() {
    //     // Arrange
    //     String validOtp = TEST_OTP;
    //     String invalidOtp = "54321";
        
    //     // Create entity with valid OTP hash
    //     OtpEntity otpEntity = createTestOtpRequest(validOtp, 0);
    //     String validOtpHash = computeOtpHash(validOtp);
    //     otpEntity.setOtpHash(validOtpHash);
    //     otpEntity.setOtpCode(validOtp);
        
    //     when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(otpEntity));

    //     // Act & Assert
    //     ValidationException exception = assertThrows(ValidationException.class, () ->
    //         otpService.validateOtp(TEST_PHONE_NUMBER, devicePublicKey, invalidOtp)
    //     );
        
    //     // Verify exception and state
    //     assertEquals("Invalid OTP", exception.getMessage());
    //     assertEquals(OtpStatus.INCOMPLETE, otpEntity.getStatus());
    //     verify(otpRequestRepository).save(otpEntity);
        
    //     // Verify the hash comparison failed
    //     String invalidOtpHash = computeOtpHash(invalidOtp);
    //     assertNotEquals(validOtpHash, invalidOtpHash, "Hashes should be different for different OTPs");
    // }

    @Test
    void testComputeHash() {
        // Arrange
        String input = "test-input";

        // Act
        String hash = otpService.computeHash(input);

        // Assert
        assertNotNull(hash);
        assertTrue(hash.matches("^[A-Za-z0-9+/=]+$")); // Base64 pattern
    }

    private OtpEntity createTestOtpRequest(String otpCode, int minutesAgo) {
        OtpEntity entity = new OtpEntity();
        entity.setPhoneNumber(TEST_PHONE_NUMBER);
        entity.setPublicKeyHash("test-public-key-hash");
        entity.setOtpHash("test-hash");
        entity.setOtpCode(otpCode);
        entity.setStatus(OtpStatus.PENDING);
        entity.setCreatedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        return entity;
    }

    private String computeOtpHash(String otp) {
        String otpJSON = String.format(
            "{\"otp\":\"%s\",\"devicePub\":%s,\"phoneNumber\":\"%s\",\"salt\":\"%s\"}",
            otp, devicePublicKey.toJSONString(), TEST_PHONE_NUMBER, TEST_SALT
        );
        try {
            org.erdtman.jcs.JsonCanonicalizer jc = new org.erdtman.jcs.JsonCanonicalizer(otpJSON);
            return otpService.computeHash(jc.getEncodedString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to canonicalize JSON", e);
        }
    }
}