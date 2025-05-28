package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.exceptions.HashComputationException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.Curve;
import com.adorsys.error.ValidationException;
import com.adorsys.error.NonceValidationException;
import com.adorsys.error.DeviceValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceRegServiceTest {

    @InjectMocks
    private DeviceRegServiceImpl deviceRegService;

    private JWK mockJWK;
    private static final String TEST_SALT = "testSalt";
    private static final String TEST_SERVER_PRIVATE_KEY = "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"test\",\"y\":\"test\",\"d\":\"test\"}";
    private static final String TEST_SERVER_PUBLIC_KEY = "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"test\",\"y\":\"test\"}";
    private static final String TEST_ISSUER = "test-issuer";
    private static final Long TEST_EXPIRATION = 3600000L;

    @BeforeEach
    void setUp() throws Exception {
        mockJWK = new ECKeyGenerator(Curve.P_256).generate();
        
        // Set required fields using ReflectionTestUtils
        ReflectionTestUtils.setField(deviceRegService, "salt", TEST_SALT);
        ReflectionTestUtils.setField(deviceRegService, "SERVER_PRIVATE_KEY_JSON", TEST_SERVER_PRIVATE_KEY);
        ReflectionTestUtils.setField(deviceRegService, "SERVER_PUBLIC_KEY_JSON", TEST_SERVER_PUBLIC_KEY);
        ReflectionTestUtils.setField(deviceRegService, "issuer", TEST_ISSUER);
        ReflectionTestUtils.setField(deviceRegService, "expirationTimeMs", TEST_EXPIRATION);
    }

    @Test
    void testInitiateDeviceRegistration_Success() {
        // Arrange
        DeviceRegInitRequest request = new DeviceRegInitRequest();
        request.setTimeStamp("2024-03-20T10:00:00");

        // Act
        String nonce = deviceRegService.initiateDeviceRegistration(mockJWK, request);

        // Assert
        assertNotNull(nonce);
        assertTrue(nonce.length() > 0);
    }

    @Test
    void testInitiateDeviceRegistration_NullPublicKey() {
        // Arrange
        DeviceRegInitRequest request = new DeviceRegInitRequest();
        request.setTimeStamp("2024-03-20T10:00:00");

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            deviceRegService.initiateDeviceRegistration(null, request)
        );
        assertEquals("Public key is required", exception.getMessage());
    }

    @Test
    void testInitiateDeviceRegistration_NullTimestamp() {
        // Arrange
        DeviceRegInitRequest request = new DeviceRegInitRequest();

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            deviceRegService.initiateDeviceRegistration(mockJWK, request)
        );
        assertEquals("Timestamp is required", exception.getMessage());
    }

    // @Test
    // void testValidateDeviceRegistration_Success() throws Exception {
    //     // Arrange
    //     String nonce = deviceRegService.initiateDeviceRegistration(mockJWK, new DeviceRegInitRequest());
    //     DeviceValidateRequest request = new DeviceValidateRequest();
    //     request.setInitiationNonce(nonce);
    //     request.setPowNonce("testPowNonce");
    //     request.setPowHash(deviceRegService.calculateSHA256("testHash"));

    //     // Act
    //     String result = deviceRegService.validateDeviceRegistration(mockJWK, request);

    //     // Assert
    //     assertNotNull(result);
    //     assertTrue(result.length() > 0);
    // }

    @Test
    void testValidateDeviceRegistration_NullPublicKey() {
        // Arrange
        DeviceValidateRequest request = new DeviceValidateRequest();
        request.setInitiationNonce("testNonce");
        request.setPowNonce("testPowNonce");
        request.setPowHash("testHash");

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            deviceRegService.validateDeviceRegistration(null, request)
        );
        assertEquals("Public key is required", exception.getMessage());
    }

    @Test
    void testValidateDeviceRegistration_NullInitiationNonce() {
        // Arrange
        DeviceValidateRequest request = new DeviceValidateRequest();
        request.setPowNonce("testPowNonce");
        request.setPowHash("testHash");

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            deviceRegService.validateDeviceRegistration(mockJWK, request)
        );
        assertEquals("Initiation nonce is required", exception.getMessage());
    }

    @Test
    void testValidateDeviceRegistration_NullPowHash() {
        // Arrange
        DeviceValidateRequest request = new DeviceValidateRequest();
        request.setInitiationNonce("testNonce");
        request.setPowNonce("testPowNonce");

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            deviceRegService.validateDeviceRegistration(mockJWK, request)
        );
        assertEquals("PoW hash is required", exception.getMessage());
    }

    @Test
    void testValidateDeviceRegistration_NullPowNonce() {
        // Arrange
        DeviceValidateRequest request = new DeviceValidateRequest();
        request.setInitiationNonce("testNonce");
        request.setPowHash("testHash");

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            deviceRegService.validateDeviceRegistration(mockJWK, request)
        );
        assertEquals("PoW nonce is required", exception.getMessage());
    }

    @Test
    void testCalculateSHA256_ValidInput() throws NoSuchAlgorithmException {
        // Arrange
        String input = "testInput";

        // Act
        String hash = deviceRegService.calculateSHA256(input);

        // Assert
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 hash length
    }

    @Test
    void testGenerateNonce_NullSaltThrowsException() {
        // Act & Assert
        assertThrows(HashComputationException.class, () -> 
            DeviceRegServiceImpl.generateNonce(null)
        );
    }

    @Test
    void testGenerateNonce_ValidSalt() {
        // Act
        String nonce = DeviceRegServiceImpl.generateNonce(TEST_SALT);

        // Assert
        assertNotNull(nonce);
        assertTrue(nonce.length() > 0);
    }
}
