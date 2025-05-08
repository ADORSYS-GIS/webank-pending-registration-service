package com.adorsys.webank;

import com.adorsys.webank.dto.AccountRecovery;
import com.adorsys.webank.dto.AccountRecoveryResponse;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.AccountRecoveryValidationRequestServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRecoveryValidationRequestRestServerTest {

    private static final String VALID_AUTH_HEADER = "Bearer valid.jwt.token";
    private static final String INVALID_AUTH_HEADER = "InvalidHeader";
    private static final String NEW_ACCOUNT_ID = "ACC123";
    private static final String RECOVERY_JWT = "recovery.jwt.token";
    private static final String OLD_ACCOUNT_ID = "OLD123";
    private static final String NEW_KYC_CERTIFICATE = "new.certificate";
    private static final String MESSAGE = "Recovery successful";

    @Mock
    private AccountRecoveryValidationRequestServiceApi service;

    @Mock
    private JWK mockPublicKey;

    @InjectMocks
    private AccountRecoveryValidationRequestRestServer restServer;

    private AccountRecovery accountRecovery;

    @BeforeEach
    void setUp() {
        accountRecovery = new AccountRecovery(NEW_ACCOUNT_ID);
    }

    @Test
    void validateRecoveryToken_WithValidRequest_ShouldReturnSuccess() {
        // Given
        try (MockedStatic<JwtValidator> jwtValidator = mockStatic(JwtValidator.class)) {
            jwtValidator.when(() -> JwtValidator.validateAndExtract(anyString(), anyString()))
                .thenReturn(mockPublicKey);
            jwtValidator.when(() -> JwtValidator.extractClaim(anyString(), anyString()))
                .thenReturn(RECOVERY_JWT);

            AccountRecoveryResponse expectedResponse = new AccountRecoveryResponse(OLD_ACCOUNT_ID, NEW_KYC_CERTIFICATE, MESSAGE);
            when(service.processRecovery(any(JWK.class), anyString(), anyString()))
                .thenReturn(expectedResponse);

            // When
            ResponseEntity<AccountRecoveryResponse> response = restServer.validateRecoveryToken(VALID_AUTH_HEADER, accountRecovery);

            // Then
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isEqualTo(expectedResponse);
        }
    }

    @Test
    void validateRecoveryToken_WithInvalidAuthHeader_ShouldReturnNull() {
        // When
        ResponseEntity<AccountRecoveryResponse> response = restServer.validateRecoveryToken(INVALID_AUTH_HEADER, accountRecovery);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNull();
    }

    @Test
    void validateRecoveryToken_WithMissingRecoveryJwt_ShouldReturnNull() {
        // Given
        try (MockedStatic<JwtValidator> jwtValidator = mockStatic(JwtValidator.class)) {
            jwtValidator.when(() -> JwtValidator.validateAndExtract(anyString(), anyString()))
                .thenReturn(mockPublicKey);
            jwtValidator.when(() -> JwtValidator.extractClaim(anyString(), anyString()))
                .thenReturn(null);

            // When
            ResponseEntity<AccountRecoveryResponse> response = restServer.validateRecoveryToken(VALID_AUTH_HEADER, accountRecovery);

            // Then
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNull();
        }
    }
} 