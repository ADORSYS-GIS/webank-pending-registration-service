package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.TokenRequest;
import com.adorsys.webank.security.HashHelper;
import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @InjectMocks
    private TokenServiceImpl tokenService;
    
    @Mock
    private HashHelper hashHelper;

    private static final String TEST_OLD_ACCOUNT_ID = "old-account-id";
    private static final String TEST_NEW_ACCOUNT_ID = "new-account-id";
    private static final String TEST_PRIVATE_KEY = "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\",\"y\":\"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\",\"d\":\"870MB6gfuTJ4HtUnUvYMyJpr5eUZNP4Bk43bVdj3eAE\"}";
    private static final String TEST_PUBLIC_KEY = "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\",\"y\":\"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\"}";
    private static final String TEST_ISSUER = "test-issuer";
    private static final Long TEST_EXPIRATION_TIME_MS = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        // Set up test configuration
        ReflectionTestUtils.setField(tokenService, "SERVER_PRIVATE_KEY_JSON", TEST_PRIVATE_KEY);
        ReflectionTestUtils.setField(tokenService, "SERVER_PUBLIC_KEY_JSON", TEST_PUBLIC_KEY);
        ReflectionTestUtils.setField(tokenService, "issuer", TEST_ISSUER);
        ReflectionTestUtils.setField(tokenService, "expirationTimeMs", TEST_EXPIRATION_TIME_MS);
        
        // Configure HashHelper mock to return a key ID when requested
        // Using lenient mode to avoid 'unnecessary stubbing' errors in tests that don't use this mock
        lenient().when(hashHelper.computeKeyId(any(ECKey.class))).thenReturn("mock-key-id");
    }

    @Test
    void requestRecoveryTokenSuccess() {
        // Given
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);

        // When
        String token = tokenService.requestRecoveryToken(request);

        // Then
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // JWT should have 3 parts
    }

    @Test
    void requestRecoveryTokenInvalidPrivateKeyReturnsNull() {
        // Given
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);
        ReflectionTestUtils.setField(tokenService, "SERVER_PRIVATE_KEY_JSON", "invalid-key");

        // When
        String token = tokenService.requestRecoveryToken(request);

        // Then
        assertNull(token);
    }

    @Test
    void requestRecoveryTokenNullRequestReturnsNull() {
        // When
        String token = tokenService.requestRecoveryToken(null);

        // Then
        assertNull(token);
    }

} 