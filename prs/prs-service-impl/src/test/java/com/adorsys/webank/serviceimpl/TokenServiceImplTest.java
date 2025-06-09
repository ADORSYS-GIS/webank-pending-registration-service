package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.adorsys.error.ValidationException;
import com.adorsys.webank.config.KeyLoader;
import com.adorsys.webank.dto.TokenRequest;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;

@ExtendWith(MockitoExtension.class)
public class TokenServiceImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(TokenServiceImplTest.class);
    
    private static final String TEST_OLD_ACCOUNT_ID = "old-account-123";
    private static final String TEST_NEW_ACCOUNT_ID = "new-account-456";
    private static final String TEST_ISSUER = "test-issuer";
    private static final long TEST_EXPIRATION_TIME_MS = 3600000L; // 1 hour
    private static final String TEST_PRIVATE_KEY = "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\",\"y\":\"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\",\"d\":\"870MB6gfuTJ4HtUnUvYMyJpr5eUZNP4Bk43bVdj3eAE\"}";
    private static final String TEST_PUBLIC_KEY = "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\",\"y\":\"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\"}";

    @Mock
    private KeyLoader keyLoader;

    @InjectMocks
    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        // Set up test configuration
        ReflectionTestUtils.setField(tokenService, "SERVER_PRIVATE_KEY_JSON", TEST_PRIVATE_KEY);
        ReflectionTestUtils.setField(tokenService, "SERVER_PUBLIC_KEY_JSON", TEST_PUBLIC_KEY);
        ReflectionTestUtils.setField(tokenService, "issuer", TEST_ISSUER);
        ReflectionTestUtils.setField(tokenService, "expirationTimeMs", TEST_EXPIRATION_TIME_MS);
    }

    @Test
    void requestRecoveryToken_Success() throws ParseException, JOSEException {
        // Arrange
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);

        // Act
        String token = tokenService.requestRecoveryToken(request);

        // Assert
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // JWT should have 3 parts
        
        // Verify JWT contents
        SignedJWT signedJWT = SignedJWT.parse(token);
        assertEquals(TEST_ISSUER, signedJWT.getJWTClaimsSet().getIssuer());
        assertEquals("RecoveryToken", signedJWT.getJWTClaimsSet().getSubject());
        assertEquals(TEST_OLD_ACCOUNT_ID, signedJWT.getJWTClaimsSet().getClaim("oldAccountId"));
        assertEquals(TEST_NEW_ACCOUNT_ID, signedJWT.getJWTClaimsSet().getClaim("newAccountId"));
        
        // Verify expiration
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        assertNotNull(expirationTime);
        assertTrue(expirationTime.after(new Date(System.currentTimeMillis() + TEST_EXPIRATION_TIME_MS - 5000))); // Allow for small time differences
    }

    @Test
    void requestRecoveryToken_NullOldAccountId() {
        // Arrange
        TokenRequest request = new TokenRequest(null, TEST_NEW_ACCOUNT_ID);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            tokenService.requestRecoveryToken(request)
        );
        assertEquals("Old account ID is required", exception.getMessage());
    }

    @Test
    void requestRecoveryToken_EmptyOldAccountId() {
        // Arrange
        TokenRequest request = new TokenRequest("", TEST_NEW_ACCOUNT_ID);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            tokenService.requestRecoveryToken(request)
        );
        assertEquals("Old account ID is required", exception.getMessage());
    }

    @Test
    void requestRecoveryToken_NullNewAccountId() {
        // Arrange
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, null);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            tokenService.requestRecoveryToken(request)
        );
        assertEquals("New account ID is required", exception.getMessage());
    }

    @Test
    void requestRecoveryToken_EmptyNewAccountId() {
        // Arrange
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, "");

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            tokenService.requestRecoveryToken(request)
        );
        assertEquals("New account ID is required", exception.getMessage());
    }

    @Test
    void requestRecoveryToken_InvalidPrivateKey() {
        // Arrange
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);
        ReflectionTestUtils.setField(tokenService, "SERVER_PRIVATE_KEY_JSON", "invalid-key");

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            tokenService.requestRecoveryToken(request)
        );
    }

    @Test
    void requestRecoveryToken_InvalidPublicKey() {
        // Arrange
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);
        ReflectionTestUtils.setField(tokenService, "SERVER_PUBLIC_KEY_JSON", "invalid-key");

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            tokenService.requestRecoveryToken(request)
        );
    }

    @Test
    void requestRecoveryToken_InvalidExpirationTime() throws ParseException {
        // Arrange
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);
        ReflectionTestUtils.setField(tokenService, "expirationTimeMs", -1000L);

        // Act
        String token = tokenService.requestRecoveryToken(request);

        // Assert
        assertNotNull(token);
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        assertNotNull(expirationTime);
        assertTrue(expirationTime.before(new Date()));
    }

    @Test
    void requestRecoveryToken_NullRequest() {
        // Act & Assert
        assertThrows(ValidationException.class, () ->
            tokenService.requestRecoveryToken(null)
        );
    }
} 