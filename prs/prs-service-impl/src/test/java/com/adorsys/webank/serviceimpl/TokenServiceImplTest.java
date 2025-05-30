package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.config.*;
import com.adorsys.webank.dto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.*;
import java.text.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nimbusds.jose.JOSEException;


public class TokenServiceImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(TokenServiceImplTest.class);

    @Mock
    private KeyLoader keyLoader;

    @InjectMocks
    private TokenServiceImpl tokenService;



    @BeforeEach
    public void setup() throws ParseException, JOSEException {
        MockitoAnnotations.openMocks(this);

        // Generate EC key pair for test
        ECKey generatedKey = new ECKeyGenerator(Curve.P_256)
                .keyUse(KeyUse.SIGNATURE)
                .keyID("123")
                .generate();
        
        ECKey privateKey = generatedKey.toECPrivateKey() != null ? generatedKey : null;
        ECKey publicKey = generatedKey.toPublicJWK();
        
        when(keyLoader.loadPrivateKey()).thenReturn(privateKey);
        when(keyLoader.loadPublicKey()).thenReturn(publicKey);



        ReflectionTestUtils.setField(tokenService, "issuer", "webank-test");
        ReflectionTestUtils.setField(tokenService, "expirationTimeMs", 60000L); // 1 min

}

    @Test
    public void testGenerateRecoveryTokenSuccess() {
        TokenRequest request = new TokenRequest("newAcc456", "oldAcc123");

        String token = tokenService.requestRecoveryToken(request);

        assertNotNull(token, "Token should not be null");

        // Parse and validate the token contents
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            assertEquals("webank-test", claims.getIssuer());
            assertEquals("RecoveryToken", claims.getSubject());
            assertEquals("oldAcc123", claims.getStringClaim("oldAccountId"));
            assertEquals("newAcc456", claims.getStringClaim("newAccountId"));
        } catch (Exception e) {
            LOG.error("Error parsing JWT token", e);
            fail("Token should be parsable and contain valid claims");
        }
    }

    @Test
    public void testGenerateRecoveryTokenErrorHandling() throws ParseException {
        // Arrange
        when(keyLoader.loadPrivateKey()).thenThrow(new RuntimeException("Key load failure"));
        TokenRequest request = new TokenRequest("acc1", "acc2");

        // Act & Assert
        String result = tokenService.requestRecoveryToken(request);
        assertNull(result, "Token should be null when private key loading fails");
        verify(keyLoader).loadPrivateKey();
    }

    @Test
    public void testGenerateRecoveryTokenWhenPrivateKeyLoadingFails() throws ParseException {
        // Arrange
        when(keyLoader.loadPrivateKey()).thenThrow(new RuntimeException("Key load failure"));
        TokenRequest request = new TokenRequest("acc1", "acc2");

        // Act
        String result = tokenService.requestRecoveryToken(request);

        // Assert
        assertNull(result, "Token should be null when private key loading fails");
        verify(keyLoader).loadPrivateKey();
        verify(keyLoader, never()).loadPublicKey();
    }
}






