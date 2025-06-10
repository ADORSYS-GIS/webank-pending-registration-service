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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import com.adorsys.error.ValidationException;
import com.adorsys.webank.config.SecurityUtils;
import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.projection.OtpProjection;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpServiceImplTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;

    @InjectMocks
    private OtpServiceImpl otpService;

    private ECKey deviceKey;
    private static final String TEST_PHONE_NUMBER = "+1234567890";
    private static final String TEST_SALT = "test-salt";
    private static final String TEST_OTP = "12345";
    private static final UUID TEST_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        deviceKey = new ECKeyGenerator(Curve.P_256).generate();
        ReflectionTestUtils.setField(otpService, "salt", TEST_SALT);
        
        // Set up MDC for logging
        MDC.put("correlationId", "test-correlation-id");
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
    void testSendOtp_Success_NewRecord() {
        // Arrange
        OtpServiceImpl spyService = spy(otpService);
        doReturn(TEST_OTP).when(spyService).generateOtp();
        when(otpRequestRepository.updateOtpByPublicKeyHash(any(), any(), any(), any())).thenReturn(0);
        when(otpRequestRepository.save(any(OtpEntity.class))).thenReturn(new OtpEntity());

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);

            // Act
            String otpHash = spyService.sendOtp(TEST_PHONE_NUMBER);

            // Assert
            assertNotNull(otpHash);
            verify(otpRequestRepository).save(any(OtpEntity.class));
        }
    }

    @Test
    void testSendOtp_Success_UpdateExisting() {
        // Arrange
        OtpServiceImpl spyService = spy(otpService);
        doReturn(TEST_OTP).when(spyService).generateOtp();
        when(otpRequestRepository.updateOtpByPublicKeyHash(any(), any(), any(), any())).thenReturn(1);
        
        OtpProjection existingProjection = mock(OtpProjection.class);
        when(existingProjection.getPhoneNumber()).thenReturn(TEST_PHONE_NUMBER);
        when(existingProjection.getPublicKeyHash()).thenReturn("test-hash");
        when(existingProjection.getStatus()).thenReturn(OtpStatus.PENDING);
        when(existingProjection.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(existingProjection));
        when(otpRequestRepository.save(any(OtpEntity.class))).thenReturn(new OtpEntity());

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);

            // Act
            String otpHash = spyService.sendOtp(TEST_PHONE_NUMBER);

            // Assert
            assertNotNull(otpHash);
            verify(otpRequestRepository).save(any(OtpEntity.class));
        }
    }

    @Test
    void testSendOtp_UpdatedRecordNotFound() {
        // Arrange
        OtpServiceImpl spyService = spy(otpService);
        doReturn(TEST_OTP).when(spyService).generateOtp();
        when(otpRequestRepository.updateOtpByPublicKeyHash(any(), any(), any(), any())).thenReturn(1);
        when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);

            // Act & Assert
            assertThrows(FailedToSendOTPException.class, () ->
                spyService.sendOtp(TEST_PHONE_NUMBER)
            );
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
    void testSendOtp_EmptyPhoneNumber() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            otpService.sendOtp("")
        );
        verify(otpRequestRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void testValidateOtp_Success() {
        // Arrange
        String otpHash = computeOtpHash(TEST_OTP);
        OtpProjection otpProjection = mock(OtpProjection.class);
        when(otpProjection.getId()).thenReturn(TEST_ID);
        when(otpProjection.getOtpHash()).thenReturn(otpHash);
        when(otpProjection.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(otpProjection.getPhoneNumber()).thenReturn(TEST_PHONE_NUMBER);
        when(otpProjection.getPublicKeyHash()).thenReturn("test-hash");
        when(otpProjection.getStatus()).thenReturn(OtpStatus.PENDING);
        
        OtpEntity otpEntity = new OtpEntity();
        when(otpRequestRepository.findById(TEST_ID)).thenReturn(Optional.of(otpEntity));
        when(otpRequestRepository.save(any(OtpEntity.class))).thenReturn(otpEntity);
        
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
    void testValidateOtp_InvalidOtp() {
        // Arrange
        String correctOtpHash = computeOtpHash("54321"); // Different OTP
        OtpProjection otpProjection = mock(OtpProjection.class);
        when(otpProjection.getId()).thenReturn(TEST_ID);
        when(otpProjection.getOtpHash()).thenReturn(correctOtpHash);
        when(otpProjection.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(otpProjection.getPhoneNumber()).thenReturn(TEST_PHONE_NUMBER);
        when(otpProjection.getPublicKeyHash()).thenReturn("test-hash");
        when(otpProjection.getStatus()).thenReturn(OtpStatus.PENDING);
        
        OtpEntity otpEntity = new OtpEntity();
        when(otpRequestRepository.findById(TEST_ID)).thenReturn(Optional.of(otpEntity));
        when(otpRequestRepository.save(any(OtpEntity.class))).thenReturn(otpEntity);
        
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);
            when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(otpProjection));

            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class, () ->
                otpService.validateOtp(TEST_PHONE_NUMBER, TEST_OTP)
            );
            assertEquals("Error validating the OTP", exception.getMessage());
        }
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
    void testValidateOtp_EntityNotFound() {
        // Arrange
        OtpProjection otpProjection = mock(OtpProjection.class);
        when(otpProjection.getId()).thenReturn(TEST_ID);
        when(otpProjection.getPublicKeyHash()).thenReturn("test-hash");
        when(otpRequestRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);
            when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(otpProjection));

            // Act
            String result = otpService.validateOtp(TEST_PHONE_NUMBER, TEST_OTP);

            // Assert
            assertEquals("Error: OTP record not found", result);
            verify(otpRequestRepository, never()).save(any(OtpEntity.class));
        }
    }

    @Test
    void testValidateOtp_ExpiredOtp() {
        // Arrange
        OtpProjection otpProjection = mock(OtpProjection.class);
        when(otpProjection.getId()).thenReturn(TEST_ID);
        when(otpProjection.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(6));
        when(otpProjection.getPhoneNumber()).thenReturn(TEST_PHONE_NUMBER);
        when(otpProjection.getPublicKeyHash()).thenReturn("test-hash");
        when(otpProjection.getStatus()).thenReturn(OtpStatus.PENDING);
        when(otpProjection.getOtpHash()).thenReturn(computeOtpHash(TEST_OTP));

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(deviceKey);
            when(otpRequestRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(otpProjection));
            when(otpRequestRepository.findById(TEST_ID)).thenReturn(Optional.of(new OtpEntity()));
            when(otpRequestRepository.save(any(OtpEntity.class))).thenReturn(new OtpEntity());

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

    @Test
    void testComputeHash_ConsistentResults() {
        // Arrange
        String input = "test-input";

        // Act
        String hash1 = otpService.computeHash(input);
        String hash2 = otpService.computeHash(input);

        // Assert
        assertEquals(hash1, hash2);
    }

    @Test
    void testComputeHash_NullInput() {
        // Act & Assert
        assertThrows(NullPointerException.class, () ->
            otpService.computeHash(null)
        );
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