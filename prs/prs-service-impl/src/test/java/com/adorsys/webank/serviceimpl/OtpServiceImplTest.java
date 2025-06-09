package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.adorsys.error.ValidationException;
import com.adorsys.webank.config.SecurityUtils;
import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.projection.OtpProjection;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;

    @InjectMocks
    private OtpServiceImpl otpService;

    private ECKey deviceKey;
    private static final String TEST_PHONE_NUMBER = "+1234567890";
    private static final String TEST_SALT = "test-salt";
    private static final String TEST_OTP = "12345";

    @BeforeEach
    void setUp() throws Exception {
        deviceKey = new ECKeyGenerator(Curve.P_256).generate();
        ReflectionTestUtils.setField(otpService, "salt", TEST_SALT);
    }

    @Test
    void testGenerateOtp() {
        // Act
        String otp = otpService.generateOtp();

        // Assert
        assertNotNull(otp);
        assertEquals(5, otp.length());
        assertTrue(Integer.parseInt(otp) >= 10000 && Integer.parseInt(otp) <= 99999);
    }

    @Test
    void testSendOtp_Success() {
        // Arrange
        doReturn(TEST_OTP).when(otpService).generateOtp();
        when(otpRequestRepository.updateOtpByPublicKeyHash(any(), any(), any(), any())).thenReturn(0);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);

            // Act
            String otpHash = otpService.sendOtp(TEST_PHONE_NUMBER);

            // Assert
            assertNotNull(otpHash);
            verify(otpRequestRepository).save(any(OtpEntity.class));
        }
    }

    @Test
    void testSendOtp_UpdateExisting() {
        // Arrange
        doReturn(TEST_OTP).when(otpService).generateOtp();
        when(otpRequestRepository.updateOtpByPublicKeyHash(any(), any(), any(), any())).thenReturn(1);
        OtpProjection existingProjection = mock(OtpProjection.class);
        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(existingProjection));

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);

            // Act
            String otpHash = otpService.sendOtp(TEST_PHONE_NUMBER);

            // Assert
            assertNotNull(otpHash);
            verify(otpRequestRepository).save(any(OtpEntity.class));
        }
    }

    @Test
    void testSendOtp_InvalidPhoneNumber() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            otpService.sendOtp("invalid-phone")
        );
        verify(otpRequestRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void testSendOtp_NullPhoneNumber() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            otpService.sendOtp(null)
        );
        verify(otpRequestRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void testValidateOtp_Success() {
        // Arrange
        String otpHash = computeOtpHash(TEST_OTP);
        OtpProjection otpProjection = mock(OtpProjection.class);
        when(otpProjection.getOtpHash()).thenReturn(otpHash);
        when(otpProjection.getCreatedAt()).thenReturn(LocalDateTime.now());
        
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);
            when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(otpProjection));

            // Act
            String result = otpService.validateOtp(TEST_PHONE_NUMBER, TEST_OTP);

            // Assert
            assertEquals("Otp Validated Successfully", result);
            verify(otpRequestRepository).save(any(OtpEntity.class));
        }
    }

    @Test
    void testValidateOtp_NullPhoneNumber() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            otpService.validateOtp(null, TEST_OTP)
        );
        assertEquals("Phone number is required", exception.getMessage());
        verify(otpRequestRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void testValidateOtp_NullOtp() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            otpService.validateOtp(TEST_PHONE_NUMBER, null)
        );
        assertEquals("OTP is required", exception.getMessage());
        verify(otpRequestRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void testValidateOtp_NoRequestFound() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);
            when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.empty());

            // Act
            String result = otpService.validateOtp(TEST_PHONE_NUMBER, TEST_OTP);

            // Assert
            assertEquals("No OTP request found for this public key", result);
            verify(otpRequestRepository, never()).save(any(OtpEntity.class));
        }
    }

    @Test
    void testValidateOtp_ExpiredOtp() {
        // Arrange
        OtpProjection otpProjection = mock(OtpProjection.class);
        when(otpProjection.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(6));

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);
            when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(otpProjection));

            // Act
            String result = otpService.validateOtp(TEST_PHONE_NUMBER, TEST_OTP);

            // Assert
            assertEquals("OTP expired. Request a new one.", result);
            verify(otpRequestRepository).save(any(OtpEntity.class));
        }
    }

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

    private String computeOtpHash(String otp) {
        String otpJSON = String.format(
            "{\"otp\":\"%s\",\"devicePub\":%s,\"phoneNumber\":\"%s\",\"salt\":\"%s\"}",
            otp, deviceKey.toJSONString(), TEST_PHONE_NUMBER, TEST_SALT
        );
        try {
            org.erdtman.jcs.JsonCanonicalizer jc = new org.erdtman.jcs.JsonCanonicalizer(otpJSON);
            return otpService.computeHash(jc.getEncodedString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to canonicalize JSON", e);
        }
    }
}