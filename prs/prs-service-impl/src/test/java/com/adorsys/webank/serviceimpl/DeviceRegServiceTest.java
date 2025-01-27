package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.exceptions.HashComputationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRegServiceTest {

    private DeviceRegServiceImpl deviceRegService;

    @BeforeEach
    void setUp() {
        deviceRegService = new DeviceRegServiceImpl();
        ReflectionTestUtils.setField(deviceRegService, "salt", "test-salt");
    }

    @Test
    void testInitiateDeviceRegistration() {
        String jwtToken = "dummyJwtToken";
        DeviceRegInitRequest request = new DeviceRegInitRequest();

        String nonce = deviceRegService.initiateDeviceRegistration(jwtToken, request);

        assertNotNull(nonce);
        assertFalse(nonce.isEmpty());
    }

    @Test
    void testValidateDeviceRegistration_SuccessfulValidation() throws Exception {
        String jwtToken = "dummyJwtToken";
        String initiationNonce = DeviceRegServiceImpl.generateNonce("test-salt");
        String powNonce = "testPowNonce";
        String pubKey = "publickey";
        String powHash = deviceRegService.calculateSHA256(initiationNonce + ":" + pubKey + ":" + powNonce);

        DeviceValidateRequest request = new DeviceValidateRequest();
        request.setInitiationNonce(initiationNonce);
        request.setPowNonce(powNonce);
        request.setPowHash(powHash);

        String result = deviceRegService.validateDeviceRegistration(jwtToken, request);

        assertEquals("Successfull validation", result);
    }

    @Test
    void testValidateDeviceRegistration_InvalidNonce() {
        String initiationNonce = "invalidNonce";
        String powNonce = "testPowNonce";
        String pubKey = "publickey";

        String powHash;
        try {
            powHash = deviceRegService.calculateSHA256(initiationNonce + ":" + pubKey + ":" + powNonce);
        } catch (Exception e) {
            throw new HashComputationException("Error calculating hash");
        }

        DeviceValidateRequest request = new DeviceValidateRequest();
        request.setInitiationNonce(initiationNonce);
        request.setPowNonce(powNonce);
        request.setPowHash(powHash);

        String jwtToken = "dummyJwtToken";
        String result = deviceRegService.validateDeviceRegistration(jwtToken, request);

        assertEquals("Error: Registration time elapsed, please try again", result);
    }

    @Test
    void testValidateDeviceRegistration_InvalidPowHash() throws Exception {
        String jwtToken = "dummyJwtToken";
        String initiationNonce = DeviceRegServiceImpl.generateNonce("test-salt");
        String powNonce = "testPowNonce";
        String powHash = "invalidHash";

        DeviceValidateRequest request = new DeviceValidateRequest();
        request.setInitiationNonce(initiationNonce);
        request.setPowNonce(powNonce);
        request.setPowHash(powHash);

        String result = deviceRegService.validateDeviceRegistration(jwtToken, request);

        assertEquals("Error: Verification of PoW failed", result);
    }

    @Test
    void testCalculateSHA256() throws Exception {
        String input = "testInput";

        String hash = deviceRegService.calculateSHA256(input);

        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }

    @Test
    void testGenerateNonce() {
        String nonce = DeviceRegServiceImpl.generateNonce("test-salt");

        assertNotNull(nonce);
        assertFalse(nonce.isEmpty());
    }

    @Test
    void testGenerateNonce_HashComputationException() {
        String invalidSalt = null;

        // Verify the exception is thrown when salt is null
        HashComputationException exception = assertThrows(
                HashComputationException.class,
                () -> DeviceRegServiceImpl.generateNonce(invalidSalt)
        );

        // Optionally verify the exception message
        assertEquals("Salt cannot be null", exception.getMessage());
    }
}
