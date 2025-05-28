package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.TokenRequest;
import com.adorsys.error.ValidationException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.ParseException;
import java.util.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @InjectMocks
    private TokenServiceImpl tokenService;

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
    }

    // @Test
    // void requestRecoveryToken_Success() throws ParseException, JOSEException {
    //     // Arrange
    //     TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);

    //     // Act
    //     String token = tokenService.requestRecoveryToken(request);

    //     // Assert
    //     assertNotNull(token);
    //     assertTrue(token.split("\\.").length == 3); // JWT should have 3 parts
        
    //     // Verify JWT contents
    //     SignedJWT signedJWT = SignedJWT.parse(token);
    //     assertEquals(TEST_ISSUER, signedJWT.getJWTClaimsSet().getIssuer());
    //     assertEquals("RecoveryToken", signedJWT.getJWTClaimsSet().getSubject());
    //     assertEquals(TEST_OLD_ACCOUNT_ID, signedJWT.getJWTClaimsSet().getClaim("oldAccountId"));
    //     assertEquals(TEST_NEW_ACCOUNT_ID, signedJWT.getJWTClaimsSet().getClaim("newAccountId"));
        
    //     // Verify expiration
    //     Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
    //     assertNotNull(expirationTime);
    //     assertTrue(expirationTime.after(new Date(System.currentTimeMillis() + TEST_EXPIRATION_TIME_MS - 5000))); // Allow for small time differences
    // }

    @Test
    void requestRecoveryToken_NullOldAccountId() {
        // Arrange
        TokenRequest request = new TokenRequest(null, TEST_NEW_ACCOUNT_ID);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            tokenService.requestRecoveryToken(request)
        );
        assertEquals("New account ID is required", exception.getMessage());
    }

    @Test
    void requestRecoveryToken_EmptyOldAccountId() {
        // Arrange
        TokenRequest request = new TokenRequest("", TEST_NEW_ACCOUNT_ID);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            tokenService.requestRecoveryToken(request)
        );
        assertEquals("New account ID is required", exception.getMessage());
    }

    @Test
    void requestRecoveryToken_NullNewAccountId() {
        // Arrange
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, null);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            tokenService.requestRecoveryToken(request)
        );
        assertEquals("Old account ID is required", exception.getMessage());
    }

    @Test
    void requestRecoveryToken_EmptyNewAccountId() {
        // Arrange
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, "");

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
            tokenService.requestRecoveryToken(request)
        );
        assertEquals("Old account ID is required", exception.getMessage());
    }

    @Test
    void requestRecoveryToken_InvalidPrivateKey() {
        // Arrange
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);
        ReflectionTestUtils.setField(tokenService, "SERVER_PRIVATE_KEY_JSON", "invalid-key");

        // Act
        String token = tokenService.requestRecoveryToken(request);

        // Assert
        assertNull(token);
    }

    @Test
    void requestRecoveryToken_InvalidPublicKey() {
        // Arrange
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);
        ReflectionTestUtils.setField(tokenService, "SERVER_PUBLIC_KEY_JSON", "invalid-key");

        // Act
        String token = tokenService.requestRecoveryToken(request);

        // Assert
        assertNull(token);
    }

    @Test
    void requestRecoveryToken_InvalidExpirationTime() {
        // Arrange
        TokenRequest request = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);
        ReflectionTestUtils.setField(tokenService, "expirationTimeMs", -1000L); // A negative value or 0 might be treated differently, using a past time instead

        // Act
        String token = tokenService.requestRecoveryToken(request);

        // Assert
        assertNotNull(token); // The service likely generates a token that is immediately expired
         try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            assertNotNull(expirationTime);
            assertTrue(expirationTime.before(new Date()));
        } catch (ParseException e) {
            fail("Failed to parse JWT: " + e.getMessage());
        }
    }

    // @Test
    // void requestRecoveryToken_NullRequest_ReturnsNull() {
    //     // When
    //     String token = tokenService.requestRecoveryToken(null);

    //     // Then
    //     assertNull(token);
    // }

} 