package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.exceptions.HashComputationException;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DeviceRegServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JWK mockJWK;
    
    private DeviceRegServiceImpl deviceRegService;


    @BeforeEach
    void setUp() {
        String testSalt = "testSalt";
        deviceRegService = new DeviceRegServiceImpl(passwordEncoder);
        ReflectionTestUtils.setField(deviceRegService, "salt", testSalt);
        
        // Use lenient() to avoid unnecessary stubbing errors
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("{argon2}encodedHash");
        lenient().when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void testInitiateDeviceRegistration() {
        DeviceRegInitRequest request = new DeviceRegInitRequest();
        String nonce = deviceRegService.initiateDeviceRegistration(mockJWK, request);
        assertNotNull(nonce);
    }

    @Test
    void testValidateDeviceRegistration_ErrorOnNonceMismatch() {
        DeviceValidateRequest request = mock(DeviceValidateRequest.class);
        when(request.getInitiationNonce()).thenReturn("invalidNonce");
        when(request.getPowNonce()).thenReturn("testNonce");
        when(request.getPowHash()).thenReturn("testHash");

        String result = deviceRegService.validateDeviceRegistration(mockJWK, request);
        assertTrue(result.contains("Error: Registration time elapsed"));
    }

    @Test
    void testCalculateHash_ValidInput() {
        String input = "testInput";
        String hash = deviceRegService.calculateHash(input);
        assertNotNull(hash);
        assertEquals("{argon2}encodedHash", hash); // Argon2 hash format
        verify(passwordEncoder).encode(input);
    }

    @Test
    void testGenerateNonce_NullSaltThrowsException() {
        assertThrows(HashComputationException.class, () -> deviceRegService.generateNonce(null));
    }


}
