package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.exceptions.HashComputationException;
import com.nimbusds.jose.jwk.JWK;
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

@ExtendWith(MockitoExtension.class)class DeviceRegServiceTest {

    @InjectMocks
    private DeviceRegServiceImpl deviceRegService;

    @Mock
    private JWK mockJWK;


    @BeforeEach
    void setUp() {
        String testSalt = "testSalt";
        ReflectionTestUtils.setField(deviceRegService, "salt", testSalt);
    }

    // @Test
    // void testInitiateDeviceRegistration() {
    //     DeviceRegInitRequest request = new DeviceRegInitRequest();
    //     String nonce = deviceRegService.initiateDeviceRegistration(mockJWK, request);
    //     assertNotNull(nonce);
    // }

    // @Test
    // void testValidateDeviceRegistration_ErrorOnNonceMismatch() throws IOException {
    //     DeviceValidateRequest request = mock(DeviceValidateRequest.class);
    //     when(request.getInitiationNonce()).thenReturn("invalidNonce");
    //     when(request.getPowNonce()).thenReturn("testNonce");
    //     when(request.getPowHash()).thenReturn("testHash");

    //     String result = deviceRegService.validateDeviceRegistration(mockJWK, request);
    //     assertTrue(result.contains("Error: Registration time elapsed"));
    // }

    @Test
    void testCalculateSHA256_ValidInput() throws NoSuchAlgorithmException {
        String input = "testInput";
        String hash = deviceRegService.calculateSHA256(input);
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 hash length
    }

    @Test
    void testGenerateNonce_NullSaltThrowsException() {
        assertThrows(HashComputationException.class, () -> DeviceRegServiceImpl.generateNonce(null));
    }


}
