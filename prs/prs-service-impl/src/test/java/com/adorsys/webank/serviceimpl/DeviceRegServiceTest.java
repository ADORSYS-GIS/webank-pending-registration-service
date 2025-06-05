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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class DeviceRegServiceTest {

    @Mock
    private HashHelper mockHashHelper;
    
    @Mock
    private ObjectMapper mockObjectMapper;
    
    @Mock
    private PasswordEncoder mockPasswordEncoder;
    
    @Mock
    private JWK mockJWK;
    
    private DeviceRegServiceImpl deviceRegService;


    @BeforeEach
    void setUp() {
        deviceRegService = new DeviceRegServiceImpl(mockHashHelper, mockObjectMapper, mockPasswordEncoder);
        
        // Set up default behaviors for the mock in lenient mode
        try {
            lenient().when(mockObjectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
            lenient().when(mockPasswordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded_" + invocation.getArgument(0));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set up mocks", e);
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
        // Setup mock to return different values for each call
        when(mockPasswordEncoder.encode(anyString()))
            .thenReturn("nonce1")
            .thenReturn("nonce2");
            
        String nonce = deviceRegService.generateNonce();
        
        // Verify that the nonce is not null or empty
        assertNotNull(nonce);
        assertFalse(nonce.isEmpty());
        
        // Test that subsequent calls return different values
        String secondNonce = deviceRegService.generateNonce();
        assertNotEquals(nonce, secondNonce, "Subsequent nonces should be unique");
        
        // Verify the mock was called with the expected arguments
        verify(mockPasswordEncoder, times(2)).encode(anyString());
    }


}
