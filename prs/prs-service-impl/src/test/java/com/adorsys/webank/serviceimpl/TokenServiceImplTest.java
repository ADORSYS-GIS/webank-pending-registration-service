package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import com.adorsys.webank.config.KeyLoader;
import com.adorsys.webank.dto.TokenRequest;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenServiceImplTest {

    @Mock
    private KeyLoader keyLoader;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private ECKey serverPrivateKey;
    private ECKey serverPublicKey;
    private static final String TEST_ISSUER = "test-issuer";
    private static final Long TEST_EXPIRATION_TIME_MS = 3600000L; // 1 hour
    private static final String TEST_OLD_ACCOUNT_ID = "old123456789";
    private static final String TEST_NEW_ACCOUNT_ID = "new123456789";
    private static final String TEST_CORRELATION_ID = "test-correlation-id";

    @BeforeEach
    void setUp() throws Exception {
        // Generate test keys
        serverPrivateKey = new ECKeyGenerator(Curve.P_256).generate();
        serverPublicKey = serverPrivateKey.toPublicJWK();

        // Set up mocks
        when(keyLoader.loadPrivateKey()).thenReturn(serverPrivateKey);
        when(keyLoader.loadPublicKey()).thenReturn(serverPublicKey);

        // Set up service properties
        ReflectionTestUtils.setField(tokenService, "issuer", TEST_ISSUER);
        ReflectionTestUtils.setField(tokenService, "expirationTimeMs", TEST_EXPIRATION_TIME_MS);

        // Set up MDC for logging
        MDC.put("correlationId", TEST_CORRELATION_ID);
    }

    @Test
    void testRequestRecoveryToken_Success() throws ParseException {
        // Arrange
        TokenRequest tokenRequest = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);

        // Act
        String token = tokenService.requestRecoveryToken(tokenRequest);

        // Assert
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // Valid JWT has 3 parts
        verify(keyLoader).loadPrivateKey();
        verify(keyLoader).loadPublicKey();
    }

    @Test
    void testRequestRecoveryToken_NullRequest() throws ParseException {
        // Act
        String token = tokenService.requestRecoveryToken(null);

        // Assert
        assertNull(token);
        verify(keyLoader, never()).loadPrivateKey();
        verify(keyLoader, never()).loadPublicKey();
    }

    @Test
    void testRequestRecoveryToken_KeyLoaderException() throws ParseException {
        // Arrange
        TokenRequest tokenRequest = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);
        when(keyLoader.loadPrivateKey()).thenThrow(new RuntimeException("Key loading failed"));

        // Act
        String token = tokenService.requestRecoveryToken(tokenRequest);

        // Assert
        assertNull(token);
        verify(keyLoader).loadPrivateKey();
        verify(keyLoader, never()).loadPublicKey();
    }

    // @Test
    // void testRequestRecoveryToken_TokenGenerationException() throws ParseException, JOSEException {
    //     // Arrange
    //     TokenRequest tokenRequest = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);
    //     when(keyLoader.loadPrivateKey()).thenReturn(serverPrivateKey);
    //     when(keyLoader.loadPublicKey()).thenReturn(serverPublicKey);

    //     try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
    //         mdcMock.when(() -> MDC.get("correlationId")).thenReturn(TEST_CORRELATION_ID);

    //         // Mock SignedJWT to throw exception during signing
    //         SignedJWT mockSignedJWT = mock(SignedJWT.class);
    //         doThrow(new JOSEException("Signing failed")).when(mockSignedJWT).sign(any(JWSSigner.class));

    //         try (MockedStatic<SignedJWT> signedJWTMock = mockStatic(SignedJWT.class)) {
    //             signedJWTMock.when(() -> new SignedJWT(any(), any())).thenReturn(mockSignedJWT);

    //             // Act
    //             String token = tokenService.requestRecoveryToken(tokenRequest);

    //             // Assert
    //             assertNull(token);
    //             verify(keyLoader).loadPrivateKey();
    //             verify(keyLoader).loadPublicKey();
    //         }
    //     }
    // }

    @Test
    void testRequestRecoveryToken_ValidTokenClaims() throws ParseException {
        // Arrange
        TokenRequest tokenRequest = new TokenRequest(TEST_OLD_ACCOUNT_ID, TEST_NEW_ACCOUNT_ID);

        // Act
        String token = tokenService.requestRecoveryToken(tokenRequest);

        // Assert
        assertNotNull(token);
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        
        assertEquals(TEST_ISSUER, claims.getIssuer());
        assertEquals("RecoveryToken", claims.getSubject());
        assertEquals(TEST_NEW_ACCOUNT_ID, claims.getStringClaim("oldAccountId"));
        assertEquals(TEST_OLD_ACCOUNT_ID, claims.getStringClaim("newAccountId"));
        
        // Verify expiration time is set correctly
        Date expirationTime = claims.getExpirationTime();
        Date issueTime = claims.getIssueTime();
        long expirationTimeSeconds = expirationTime.getTime() / 1000;
        long issueTimeSeconds = issueTime.getTime() / 1000;
        assertEquals(TEST_EXPIRATION_TIME_MS / 1000, expirationTimeSeconds - issueTimeSeconds);
    }

    @Test
    void testRequestRecoveryToken_ShortAccountIds() throws ParseException {
        // Arrange
        TokenRequest tokenRequest = new TokenRequest("123", "456");

        // Act
        String token = tokenService.requestRecoveryToken(tokenRequest);

        // Assert
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void testRequestRecoveryToken_NullAccountIds() throws ParseException {
        // Arrange
        TokenRequest tokenRequest = new TokenRequest(null, null);

        // Act
        String token = tokenService.requestRecoveryToken(tokenRequest);

        // Assert
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void testRequestRecoveryToken_EmptyAccountIds() throws ParseException {
        // Arrange
        TokenRequest tokenRequest = new TokenRequest("", "");

        // Act
        String token = tokenService.requestRecoveryToken(tokenRequest);

        // Assert
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }
} 