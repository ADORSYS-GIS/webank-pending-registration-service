package com.adorsys.webank;

import com.adorsys.webank.dto.TokenRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.TokenServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRestServerTest {

    private static final String VALID_AUTH_HEADER = "Bearer valid.jwt.token";
    private static final String INVALID_AUTH_HEADER = "InvalidHeader";
    private static final String OLD_ACCOUNT_ID = "OLD123";
    private static final String NEW_ACCOUNT_ID = "NEW123";

    @Mock
    private TokenServiceApi tokenServiceApi;

    @Mock
    private CertValidator certValidator;

    @Mock
    private JWK mockPublicKey;

    @InjectMocks
    private TokenRestServer tokenRestServer;

    private TokenRequest tokenRequest;

    @BeforeEach
    void setUp() {
        tokenRequest = new TokenRequest(NEW_ACCOUNT_ID, OLD_ACCOUNT_ID);
    }

    @Test
    void requestRecoveryToken_WithValidRequest_ShouldReturnSuccess() {
        // Given
        try (MockedStatic<JwtValidator> jwtValidator = mockStatic(JwtValidator.class)) {
            jwtValidator.when(() -> JwtValidator.validateAndExtract(anyString(), anyString(), anyString()))
                .thenReturn(mockPublicKey);
            when(certValidator.validateJWT(anyString())).thenReturn(true);
            when(tokenServiceApi.requestRecoveryToken(any(TokenRequest.class)))
                .thenReturn("Recovery token generated successfully");

            // When
            String response = tokenRestServer.requestRecoveryToken(VALID_AUTH_HEADER, tokenRequest);

            // Then
            assertThat(response).isEqualTo("Recovery token generated successfully");
        }
    }

    @Test
    void requestRecoveryToken_WithInvalidAuthHeader_ShouldReturnError() {
        // When
        String response = tokenRestServer.requestRecoveryToken(INVALID_AUTH_HEADER, tokenRequest);

        // Then
        assertThat(response).contains("Invalid JWT");
    }

    @Test
    void requestRecoveryToken_WithInvalidJWT_ShouldReturnUnauthorized() {
        // Given
        try (MockedStatic<JwtValidator> jwtValidator = mockStatic(JwtValidator.class)) {
            jwtValidator.when(() -> JwtValidator.validateAndExtract(anyString(), anyString(), anyString()))
                .thenReturn(mockPublicKey);
            when(certValidator.validateJWT(anyString())).thenReturn(false);

            // When
            String response = tokenRestServer.requestRecoveryToken(VALID_AUTH_HEADER, tokenRequest);

            // Then
            assertThat(response).isEqualTo("Unauthorized");
        }
    }
} 