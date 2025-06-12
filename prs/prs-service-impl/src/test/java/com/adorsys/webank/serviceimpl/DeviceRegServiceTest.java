package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.config.KeyLoader;
import com.adorsys.webank.config.SecurityUtils;
import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceRegServiceTest {
    private static final String TEST_CORRELATION_ID = "test-correlation-id";

    @InjectMocks
    private DeviceRegServiceImpl deviceRegService;

    @Mock
    private ECKey mockECKey;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private KeyLoader keyLoader;

    @BeforeEach
    void setUp() {
        // Initialize the service with all required dependencies
        deviceRegService = new DeviceRegServiceImpl(objectMapper, passwordEncoder, keyLoader);
    }
    
    @Test
    void testInitiateDeviceRegistration() {
        // Setup password encoder mock
        when(passwordEncoder.encode(any(CharSequence.class))).thenAnswer(invocation -> {
            String input = invocation.getArgument(0);
            return "encoded_" + input; // Simple encoding for testing
        });
        
        DeviceRegInitRequest request = new DeviceRegInitRequest();
        String nonce = deviceRegService.initiateDeviceRegistration(request);
        assertNotNull(nonce);
    }

    @Test
    void testValidateDeviceRegistration_ErrorOnNonceMismatch() {
        // Setup password encoder mock
        when(passwordEncoder.encode(any(CharSequence.class))).thenAnswer(invocation -> {
            String input = invocation.getArgument(0);
            return "encoded_" + input; // Simple encoding for testing
        });
        
        // Setup
        MDC.put("correlationId", TEST_CORRELATION_ID);
        
        // Create a valid nonce using the actual generation method
        String validNonce = deviceRegService.generateNonce();
        
        // Mock request with valid nonce but invalid proof of work
        DeviceValidateRequest request = new DeviceValidateRequest();
        request.setInitiationNonce(validNonce);
        request.setPowNonce("testNonce");
        request.setPowHash("invalid-hash"); // This will cause the validation to fail

        // Mock SecurityUtils to return the ECKey
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            // Create a real EC key for testing
            ECKey mockECKey = new ECKeyGenerator(Curve.P_256)
                .keyID("test-key-id")
                .generate();
                
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext)
                .thenReturn(mockECKey);

            // Create real dependencies
            ObjectMapper objectMapper = new ObjectMapper();
            PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
            KeyLoader keyLoader = mock(KeyLoader.class);
            
            // Mock password encoder to accept the nonce
            when(passwordEncoder.matches(anyString(), eq(validNonce))).thenReturn(true);
            
            // Create the service with real dependencies
            deviceRegService = new DeviceRegServiceImpl(objectMapper, passwordEncoder, keyLoader);
            
            // Act
            String result = deviceRegService.validateDeviceRegistration(request);
            
            // Assert - The actual implementation returns a specific error message for invalid proof of work
            assertTrue(result.contains("Error: Verification of PoW failed") || 
                      result.contains("Error: Unable to verify proof of work"), 
                "Expected error message about invalid proof of work");
                
            // Verify interactions
            verify(passwordEncoder).matches(anyString(), eq(validNonce));
            
            // Clean up
            MDC.clear();
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    void testCalculateSHA256_ValidInput() throws NoSuchAlgorithmException {
        String input = "testInput";
        String hash = deviceRegService.calculateSHA256(input);
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 hash length
    }

    @Test
    void testGenerateNonce_GeneratesValidNonce() {
        // Setup password encoder mock
        when(passwordEncoder.encode(any(CharSequence.class))).thenAnswer(invocation -> {
            String input = invocation.getArgument(0);
            return "encoded_" + input; // Simple encoding for testing
        });
        
        // Act
        String nonce = deviceRegService.generateNonce();
        
        // Assert
        assertNotNull(nonce, "Nonce should not be null");
        
        // The nonce is now a password-encoded string, so we'll just check it's not empty
        assertFalse(nonce.isEmpty(), "Nonce should not be empty");
    }
}
