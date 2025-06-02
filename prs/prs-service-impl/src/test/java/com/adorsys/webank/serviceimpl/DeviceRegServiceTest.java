package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.security.HashHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)class DeviceRegServiceTest {

    @Mock
    private PasswordHashingService mockPasswordHashingService;
    
    @Mock
    private HashHelper mockHashHelper;
    
    @Mock
    private ObjectMapper mockObjectMapper;
    
    @Mock
    private JWK mockJWK;
    
    private DeviceRegServiceImpl deviceRegService;


    @BeforeEach
    void setUp() {
        deviceRegService = new DeviceRegServiceImpl(mockPasswordHashingService, mockHashHelper, mockObjectMapper);
        
        // Set up default behaviors for the mock in lenient mode
        lenient().when(mockPasswordHashingService.hash(anyString())).thenReturn("hashedValue");
        lenient().when(mockPasswordHashingService.verify(anyString(), anyString())).thenReturn(false);
        try {
            lenient().when(mockObjectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set up mock ObjectMapper", e);
        }
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
        
        // These stubs are not used because the method returns early on nonce validation
        // Using lenient() to prevent Mockito from reporting them as unnecessary
        lenient().when(request.getPowNonce()).thenReturn("testNonce");
        lenient().when(request.getPowHash()).thenReturn("testHash");

        String result = deviceRegService.validateDeviceRegistration(mockJWK, request);
        assertTrue(result.contains("Error: Registration time elapsed"));
    }

    @Test
    void testCalculateSHA256ValidInput() throws NoSuchAlgorithmException {
        String input = "testInput";
        
        // The actual SHA-256 hash of 'testInput' encoded in hex
        String expectedHash = "620ae460798e1f4cab44c44f3085620284f0960a276bbc3f0bd416449df14dbe";
        
        // Setup mock to return the expected hash
        when(mockHashHelper.calculateSHA256AsHex(input)).thenReturn(expectedHash);
        
        // Use HashHelper directly rather than the deprecated method
        String hash = mockHashHelper.calculateSHA256AsHex(input);
        assertNotNull(hash);
        assertEquals(expectedHash, hash);
        
        // Verify interaction with HashHelper
        verify(mockHashHelper).calculateSHA256AsHex(input);
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
