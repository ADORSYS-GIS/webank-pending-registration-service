package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)class DeviceRegServiceTest {

    @Mock
    private PasswordHashingService mockPasswordHashingService;
    
    @Mock
    private JWK mockJWK;
    
    private DeviceRegServiceImpl deviceRegService;


    @BeforeEach
    void setUp() {
        deviceRegService = new DeviceRegServiceImpl(mockPasswordHashingService);
        
        // Set up default behaviors for the mock in lenient mode
        lenient().when(mockPasswordHashingService.hash(anyString())).thenReturn("hashedValue");
        lenient().when(mockPasswordHashingService.verify(anyString(), anyString())).thenReturn(false);
    }

    @Test
    void testInitiateDeviceRegistration() {
        DeviceRegInitRequest request = new DeviceRegInitRequest();
        String nonce = deviceRegService.initiateDeviceRegistration(mockJWK, request);
        assertNotNull(nonce);
    }

    @Test
    void testValidateDeviceRegistrationErrorOnNonceMismatch() throws IOException {
        DeviceValidateRequest request = mock(DeviceValidateRequest.class);
        when(request.getInitiationNonce()).thenReturn("invalidNonce");
        when(request.getPowNonce()).thenReturn("testNonce");
        when(request.getPowHash()).thenReturn("testHash");

        String result = deviceRegService.validateDeviceRegistration(mockJWK, request);
        assertTrue(result.contains("Error: Registration time elapsed"));
    }

    @Test
    void testCalculateSHA256ValidInput() throws NoSuchAlgorithmException {
        String input = "testInput";
        
        // The actual SHA-256 hash of 'testInput' encoded in hex
        String expectedHash = "620ae460798e1f4cab44c44f3085620284f0960a276bbc3f0bd416449df14dbe";
        
        String hash = deviceRegService.calculateSHA256(input);
        assertNotNull(hash);
        assertEquals(expectedHash, hash);
        
        // No need to verify interaction with mockPasswordHashingService
        // since calculateSHA256 now uses MessageDigest directly
    }

    @Test
    void testGenerateNonceGeneratesHash() {
        String expectedHash = "hashValue";
        when(mockPasswordHashingService.hash(anyString())).thenReturn(expectedHash);
        
        String nonce = deviceRegService.generateNonce();
        assertEquals(expectedHash, nonce);
        verify(mockPasswordHashingService).hash(anyString());
    }


}
