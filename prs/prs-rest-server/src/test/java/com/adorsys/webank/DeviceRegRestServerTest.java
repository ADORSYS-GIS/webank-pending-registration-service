package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.DeviceRegServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceRegRestServerTest {

    private static final String VALID_AUTH_HEADER = "Bearer valid.jwt.token";
    private static final String INVALID_AUTH_HEADER = "InvalidHeader";
    private static final String TIMESTAMP = "2024-03-14T12:00:00Z";
    private static final String INITIATION_NONCE = "nonce123";
    private static final String POW_HASH = "hash123";
    private static final String POW_NONCE = "powNonce123";

    @Mock
    private DeviceRegServiceApi deviceRegServiceApi;

    @Mock
    private JWK mockPublicKey;

    @InjectMocks
    private DeviceRegRestServer deviceRegRestServer;

    private DeviceRegInitRequest initRequest;
    private DeviceValidateRequest validateRequest;

    @BeforeEach
    void setUp() {
        initRequest = new DeviceRegInitRequest();
        initRequest.setTimeStamp(TIMESTAMP);

        validateRequest = new DeviceValidateRequest();
        validateRequest.setInitiationNonce(INITIATION_NONCE);
        validateRequest.setPowHash(POW_HASH);
        validateRequest.setPowNonce(POW_NONCE);
    }

    @Test
    void initiateDeviceRegistration_WithValidRequest_ShouldReturnSuccess() {
        // Given
        try (MockedStatic<JwtValidator> jwtValidator = mockStatic(JwtValidator.class)) {
            jwtValidator.when(() -> JwtValidator.validateAndExtract(anyString(), eq(TIMESTAMP)))
                .thenReturn(mockPublicKey);
            when(deviceRegServiceApi.initiateDeviceRegistration(any(), eq(initRequest)))
                .thenReturn("Device registration initiated successfully");

            // When
            ResponseEntity<String> response = deviceRegRestServer.initiateDeviceRegistration(VALID_AUTH_HEADER, initRequest);

            // Then
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isEqualTo("Device registration initiated successfully");
        }
    }

    @Test
    void initiateDeviceRegistration_WithInvalidAuthHeader_ShouldReturnError() {
        // When
        ResponseEntity<String> response = deviceRegRestServer.initiateDeviceRegistration(INVALID_AUTH_HEADER, initRequest);

        // Then
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).contains("Invalid JWT");
    }

    @Test
    void validateDeviceRegistration_WithValidRequest_ShouldReturnSuccess() throws IOException {
        // Given
        try (MockedStatic<JwtValidator> jwtValidator = mockStatic(JwtValidator.class)) {
            jwtValidator.when(() -> JwtValidator.validateAndExtract(anyString(), eq(INITIATION_NONCE), eq(POW_HASH), eq(POW_NONCE)))
                .thenReturn(mockPublicKey);
            when(deviceRegServiceApi.validateDeviceRegistration(any(), eq(validateRequest)))
                .thenReturn("Device registration validated successfully");

            // When
            ResponseEntity<String> response = deviceRegRestServer.validateDeviceRegistration(VALID_AUTH_HEADER, validateRequest);

            // Then
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isEqualTo("Device registration validated successfully");
        }
    }

    @Test
    void validateDeviceRegistration_WithInvalidAuthHeader_ShouldReturnError() {
        // When
        ResponseEntity<String> response = deviceRegRestServer.validateDeviceRegistration(INVALID_AUTH_HEADER, validateRequest);

        // Then
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).contains("Invalid JWT");
    }
} 