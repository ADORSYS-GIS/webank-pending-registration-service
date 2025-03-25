package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.repository.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KycCertServiceImplTest {

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @InjectMocks
    private KycCertServiceImpl kycCertService;

    private ECKey serverPublicKey; // Retained for test usage
    private static final String ISSUER = "test-issuer";
    private static final Long EXPIRATION_TIME_MS = 3600000L;

    @BeforeEach
    public void setUp() throws Exception {
        // Convert to local variables
        ECKey serverPrivateKey = new ECKeyGenerator(Curve.P_256).generate();
        serverPublicKey = serverPrivateKey.toPublicJWK(); // Class field
        String serverPrivateKeyJson = serverPrivateKey.toJSONString();
        String serverPublicKeyJson = serverPublicKey.toJSONString();

        // Inject into the service using reflection
        setField("SERVER_PRIVATE_KEY_JSON", serverPrivateKeyJson);
        setField("SERVER_PUBLIC_KEY_JSON", serverPublicKeyJson);
        setField("issuer", ISSUER);
        setField("expirationTimeMs", EXPIRATION_TIME_MS);
    }

    private void setField(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = KycCertServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(kycCertService, value);
    }

    @Test
    public void testGetCert_Success() throws Exception {
        // Arrange
        ECKey deviceKey = new ECKeyGenerator(Curve.P_256).generate();
        String deviceJwkJson = deviceKey.toJSONString();
        String publicKeyHash = kycCertService.computeHash(deviceJwkJson);

        PersonalInfoEntity entity = new PersonalInfoEntity();
        entity.setPublicKeyHash(publicKeyHash);
        entity.setStatus(PersonalInfoStatus.APPROVED);

        when(personalInfoRepository.findByPublicKeyHash(publicKeyHash)).thenReturn(Optional.of(entity));

        // Act
        String result = kycCertService.getCert(deviceKey);

        // Assert
        assertTrue(result.startsWith("Your certificate is: "));
        String jwt = result.replace("Your certificate is: ", "").trim();

        SignedJWT parsedJwt = SignedJWT.parse(jwt);
        JWSHeader header = parsedJwt.getHeader();
        JWTClaimsSet claims = parsedJwt.getJWTClaimsSet();

        // Verify header
        assertEquals(JWSAlgorithm.ES256, header.getAlgorithm());
        assertEquals(JOSEObjectType.JWT, header.getType());
        assertEquals(computeKid(serverPublicKey), header.getKeyID());

        // Verify claims
        assertEquals(ISSUER, claims.getIssuer());
        assertNotNull(claims.getIssueTime());
        assertNotNull(claims.getExpirationTime());
        assertEquals(claims.getIssueTime().getTime() + EXPIRATION_TIME_MS, claims.getExpirationTime().getTime());

        // Verify cnf claim
        assertTrue(claims.getClaims().containsKey("cnf"));
        assertTrue(((Map<?, ?>) claims.getClaim("cnf")).containsKey("jwk"));
    }

    @Test
    public void testGetCert_NotApproved() throws JOSEException {
        // Arrange
        ECKey deviceKey = new ECKeyGenerator(Curve.P_256).generate();
        String deviceJwkJson = deviceKey.toJSONString();
        String publicKeyHash = kycCertService.computeHash(deviceJwkJson);

        PersonalInfoEntity entity = new PersonalInfoEntity();
        entity.setPublicKeyHash(publicKeyHash);
        entity.setStatus(PersonalInfoStatus.PENDING);

        when(personalInfoRepository.findByPublicKeyHash(publicKeyHash)).thenReturn(Optional.of(entity));

        // Act
        String result = kycCertService.getCert(deviceKey);

        // Assert
        assertEquals("null", result);
    }

    @Test
    public void testGetCert_NotFound() throws JOSEException {
        // Arrange
        ECKey deviceKey = new ECKeyGenerator(Curve.P_256).generate();
        when(personalInfoRepository.findByPublicKeyHash(anyString())).thenReturn(Optional.empty());

        // Act
        String result = kycCertService.getCert(deviceKey);

        // Assert
        assertEquals("null", result);
    }

    @Test
    public void testComputeHash() {
        // Arrange
        String input = "test";
        String expectedHash = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

        // Act
        String actualHash = kycCertService.computeHash(input);

        // Assert
        assertEquals(expectedHash, actualHash);
    }

    @Test
    public void testGetCert_InvalidServerKey() throws Exception {
        // Arrange
        ECKey deviceKey = new ECKeyGenerator(Curve.P_256).generate();
        String deviceJwkJson = deviceKey.toJSONString();
        String publicKeyHash = kycCertService.computeHash(deviceJwkJson);

        PersonalInfoEntity entity = new PersonalInfoEntity();
        entity.setPublicKeyHash(publicKeyHash);
        entity.setStatus(PersonalInfoStatus.APPROVED);

        when(personalInfoRepository.findByPublicKeyHash(publicKeyHash)).thenReturn(Optional.of(entity));

        // Replace server's private key with a public key (no 'd' parameter)
        ECKey invalidPrivateKey = serverPublicKey;
        String invalidPrivateKeyJson = invalidPrivateKey.toJSONString();
        setField("SERVER_PRIVATE_KEY_JSON", invalidPrivateKeyJson);

        // Act
        String result = kycCertService.getCert(deviceKey);

        // Assert
        assertEquals("null", result);
    }

    private String computeKid(ECKey key) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(key.toPublicJWK().toJSONString().getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
