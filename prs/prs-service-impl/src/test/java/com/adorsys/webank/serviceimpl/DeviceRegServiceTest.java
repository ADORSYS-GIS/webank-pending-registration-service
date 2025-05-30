package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.config.*;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.exceptions.*;
import com.nimbusds.jose.jwk.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.test.util.*;

import java.io.*;
import java.security.*;
import java.text.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)class DeviceRegServiceTest {

    @InjectMocks
    private DeviceRegServiceImpl deviceRegService;

    @Mock
    private ECKey mockECKey;


    @BeforeEach
    void setUp() {
        String testSalt = "testSalt";
        ReflectionTestUtils.setField(deviceRegService, "salt", testSalt);
    }

    @Test
    void testInitiateDeviceRegistration() {
        DeviceRegInitRequest request = new DeviceRegInitRequest();
        String nonce = deviceRegService.initiateDeviceRegistration(request);
        assertNotNull(nonce);
    }

    @Test
    void testValidateDeviceRegistration_ErrorOnNonceMismatch() throws IOException, ParseException {
        DeviceValidateRequest request = mock(DeviceValidateRequest.class);
        when(request.getInitiationNonce()).thenReturn("invalidNonce");
        when(request.getPowNonce()).thenReturn("testNonce");
        when(request.getPowHash()).thenReturn("testHash");

        // Mock SecurityUtils to return the ECKey
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext)
                .thenReturn(mockECKey);

            String result = deviceRegService.validateDeviceRegistration(request);
            assertTrue(result.contains("Error: Registration time elapsed"));
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
    void testGenerateNonce_NullSaltThrowsException() {
        assertThrows(HashComputationException.class, () -> DeviceRegServiceImpl.generateNonce(null));
    }


}
