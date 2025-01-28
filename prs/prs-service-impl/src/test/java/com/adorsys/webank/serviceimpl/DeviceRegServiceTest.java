//package com.adorsys.webank.serviceimpl;
//
//import com.adorsys.webank.dto.DeviceRegInitRequest;
//import com.adorsys.webank.dto.DeviceValidateRequest;
//import com.adorsys.webank.exceptions.HashComputationException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class DeviceRegServiceTest {
//
//    private DeviceRegServiceImpl deviceRegService;
//
//    @BeforeEach
//    void setUp() {
//        deviceRegService = new DeviceRegServiceImpl();
//        ReflectionTestUtils.setField(deviceRegService, "salt", "test-salt");
//        ReflectionTestUtils.setField(deviceRegService, "SERVER_PRIVATE_KEY_JSON", "{your-private-key-json-here}");
//        ReflectionTestUtils.setField(deviceRegService, "SERVER_PUBLIC_KEY_JSON", "{your-public-key-json-here}");
//    }
//
//    @Test
//    void testInitiateDeviceRegistration() {
//        String jwtToken = "dummyJwtToken";
//        DeviceRegInitRequest request = new DeviceRegInitRequest();
//
//        String nonce = deviceRegService.initiateDeviceRegistration(jwtToken, request);
//
//        assertNotNull(nonce);
//        assertFalse(nonce.isEmpty());
//    }
//
////    @Test
////    void testValidateDeviceRegistration_SuccessfulValidation() throws Exception {
////        String jwtToken = "dummyJwtToken";
////        String initiationNonce = DeviceRegServiceImpl.generateNonce("test-salt");
////        String powNonce = "testPowNonce";
////        String pubKey = "publickey";
////        String powHash = deviceRegService.calculateSHA256(initiationNonce + ":" + pubKey + ":" + powNonce);
////
////        DeviceValidateRequest request = new DeviceValidateRequest();
////        request.setInitiationNonce(initiationNonce);
////        request.setPowNonce(powNonce);
////        request.setPowHash(powHash);
////
////        String result = deviceRegService.validateDeviceRegistration(jwtToken, request);
////
////        assertTrue(result.startsWith("Device successfully verified"));
////    }
//
//    @Test
//    void testValidateDeviceRegistration_InvalidNonce() throws Exception {
//        String initiationNonce = "invalidNonce";
//        String powNonce = "testPowNonce";
//        String pubKey = "publickey";
//
//        String powHash = deviceRegService.calculateSHA256(initiationNonce + ":" + pubKey + ":" + powNonce);
//
//        DeviceValidateRequest request = new DeviceValidateRequest();
//        request.setInitiationNonce(initiationNonce);
//        request.setPowNonce(powNonce);
//        request.setPowHash(powHash);
//
//        String jwtToken = "dummyJwtToken";
//        String result = deviceRegService.validateDeviceRegistration(jwtToken, request);
//
//        assertEquals("Error: Registration time elapsed, please try again", result);
//    }
//
//    @Test
//    void testValidateDeviceRegistration_InvalidPowHash() throws Exception {
//        String jwtToken = "dummyJwtToken";
//        String initiationNonce = DeviceRegServiceImpl.generateNonce("test-salt");
//        String powNonce = "testPowNonce";
//        String powHash = "invalidHash";
//
//        DeviceValidateRequest request = new DeviceValidateRequest();
//        request.setInitiationNonce(initiationNonce);
//        request.setPowNonce(powNonce);
//        request.setPowHash(powHash);
//
//        String result = deviceRegService.validateDeviceRegistration(jwtToken, request);
//
//        assertEquals("Error: Verification of PoW failed", result);
//    }
//
//    @Test
//    void testCalculateSHA256() throws Exception {
//        String input = "testInput";
//
//        String hash = deviceRegService.calculateSHA256(input);
//
//        assertNotNull(hash);
//        assertFalse(hash.isEmpty());
//    }
//
//    @Test
//    void testGenerateNonce() {
//        String nonce = DeviceRegServiceImpl.generateNonce("test-salt");
//
//        assertNotNull(nonce);
//        assertFalse(nonce.isEmpty());
//    }
//
//    @Test
//    void testGenerateNonce_HashComputationException() {
//        String invalidSalt = null;
//
//        // Verify the exception is thrown when salt is null
//        HashComputationException exception = assertThrows(
//                HashComputationException.class,
//                () -> DeviceRegServiceImpl.generateNonce(invalidSalt)
//        );
//
//        // Optionally verify the exception message
//        assertEquals("Salt cannot be null", exception.getMessage());
//    }
//
////    @Test
////    void testGenerateDeviceCertificate() {
////        String devicePublicKey = "dummyPublicKey";
////
////        String certificate = deviceRegService.generateDeviceCertificate(devicePublicKey);
////
////        assertNotNull(certificate);
////        assertTrue(certificate.startsWith("eyJ"));
////    }
//
//    @Test
//    void testGenerateDeviceCertificate_Exception() {
//        // Mocking a valid private key JSON
//        String validPrivateKeyJson = "{ \"kty\": \"RSA\", \"n\": \"dummyModulus\", \"e\": \"dummyExponent\" }";
//        ReflectionTestUtils.setField(deviceRegService, "SERVER_PRIVATE_KEY_JSON", validPrivateKeyJson);
//
//        String devicePublicKey = "dummyPublicKey";
//        RuntimeException exception = assertThrows(
//                RuntimeException.class,
//                () -> deviceRegService.generateDeviceCertificate(devicePublicKey)
//        );
//
//        assertTrue(exception.getMessage().contains("Error generating device certificate"));
//    }
//
//}
