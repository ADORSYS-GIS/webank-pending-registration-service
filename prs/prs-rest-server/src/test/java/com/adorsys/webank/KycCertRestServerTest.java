package com.adorsys.webank;

import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.KycCertServiceApi;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycCertRestServerTest {

    private static final String INVALID_AUTH_HEADER = "InvalidHeader";
    private static final String ACCOUNT_ID = "1234567890";

    @Mock
    private KycCertServiceApi kycCertService;

    @Mock
    private CertValidator certValidator;

    @Mock
    private JWK mockPublicKey;

    @InjectMocks
    private KycCertRestServer kycCertRestServer;

    private String validAuthHeader;

    @BeforeEach
    void setUp() throws Exception {
        // Generate EC key pair
        ECKey ecKey = new ECKeyGenerator(Curve.P_256).generate();

        // Create JWT with JWK in header
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
            .jwk(ecKey.toPublicJWK())
            .build();

        // Create hash for the payload using the concatenated parameters
        String hash = JwtValidator.hashPayload("");

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject("1234567890")
            .claim("name", "John Doe")
            .claim("iat", 1516239022)
            .claim("hash", hash)
            .build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(new ECDSASigner(ecKey));

        validAuthHeader = "Bearer " + signedJWT.serialize();

        // Use lenient() for stubs that might not be used in all tests
        lenient().when(certValidator.validateJWT(validAuthHeader.replace("Bearer ", ""))).thenReturn(true);
        lenient().when(certValidator.validateJWT(INVALID_AUTH_HEADER)).thenReturn(false);
    }

    @Test
    void getCert_WithValidRequest_ShouldReturnSuccess() {
        // Given
        when(kycCertService.getCert(any(JWK.class), eq(ACCOUNT_ID))).thenReturn("Certificate generated successfully");

        // When
        String response = kycCertRestServer.getCert(validAuthHeader, ACCOUNT_ID);

        // Then
        assertThat(response).isEqualTo("Certificate generated successfully");
    }

    @Test
    void getCert_WithInvalidAuthHeader_ShouldReturnError() {
        // When
        String response = kycCertRestServer.getCert(INVALID_AUTH_HEADER, ACCOUNT_ID);

        // Then
        assertThat(response).isEqualTo("Invalid JWT: Authorization header must start with 'Bearer '");
    }

    @Test
    void getCert_WithInvalidJWT_ShouldReturnUnauthorized() {
        // Given
        when(certValidator.validateJWT(validAuthHeader.replace("Bearer ", ""))).thenReturn(false);

        // When
        String response = kycCertRestServer.getCert(validAuthHeader, ACCOUNT_ID);

        // Then
        assertThat(response).isEqualTo("Unauthorized");
    }
} 