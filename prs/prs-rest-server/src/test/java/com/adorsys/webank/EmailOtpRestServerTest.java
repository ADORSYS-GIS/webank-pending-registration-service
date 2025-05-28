package com.adorsys.webank;

import com.adorsys.webank.dto.EmailOtpRequest;
import com.adorsys.webank.dto.EmailOtpValidationRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.EmailOtpServiceApi;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailOtpRestServerTest {

    private static final String INVALID_AUTH_HEADER = "InvalidHeader";
    private static final String EMAIL = "test@example.com";
    private static final String ACCOUNT_ID = "1234567890";
    private static final String OTP = "123456";

    @Mock
    private EmailOtpServiceApi emailOtpService;

    @Mock
    private CertValidator certValidator;

    @Mock
    private JWK mockPublicKey;

    @InjectMocks
    private EmailOtpRestServer emailOtpRestServer;

    private String validAuthHeader;
    private String validValidateEmailOtpAuthHeader;
    private EmailOtpRequest emailOtpRequest;
    private EmailOtpValidationRequest emailOtpValidationRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Generate EC key pair
        ECKey ecKey = new ECKeyGenerator(Curve.P_256).generate();

        // Create JWT with JWK in header
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
            .jwk(ecKey.toPublicJWK())
            .build();

        // Create hash for sendEmailOtp using email and accountId
        String sendEmailOtpHash = JwtValidator.hashPayload(EMAIL + ACCOUNT_ID);

        JWTClaimsSet sendEmailOtpClaimsSet = new JWTClaimsSet.Builder()
            .subject("1234567890")
            .claim("name", "John Doe")
            .claim("iat", 1516239022)
            .claim("hash", sendEmailOtpHash)
            .build();

        SignedJWT sendEmailOtpJWT = new SignedJWT(header, sendEmailOtpClaimsSet);
        sendEmailOtpJWT.sign(new ECDSASigner(ecKey));

        // Create hash for validateEmailOtp using email, otp, and accountId
        String validateEmailOtpHash = JwtValidator.hashPayload(EMAIL + OTP + ACCOUNT_ID);

        JWTClaimsSet validateEmailOtpClaimsSet = new JWTClaimsSet.Builder()
            .subject("1234567890")
            .claim("name", "John Doe")
            .claim("iat", 1516239022)
            .claim("hash", validateEmailOtpHash)
            .build();

        SignedJWT validateEmailOtpJWT = new SignedJWT(header, validateEmailOtpClaimsSet);
        validateEmailOtpJWT.sign(new ECDSASigner(ecKey));

        validAuthHeader = "Bearer " + sendEmailOtpJWT.serialize();
        validValidateEmailOtpAuthHeader = "Bearer " + validateEmailOtpJWT.serialize();

        // Use lenient() for stubs that might not be used in all tests
        lenient().when(certValidator.validateJWT(validAuthHeader.replace("Bearer ", ""))).thenReturn(true);
        lenient().when(certValidator.validateJWT(validValidateEmailOtpAuthHeader.replace("Bearer ", ""))).thenReturn(true);
        lenient().when(certValidator.validateJWT(INVALID_AUTH_HEADER)).thenReturn(false);

        emailOtpRequest = new EmailOtpRequest(EMAIL, ACCOUNT_ID);
        emailOtpValidationRequest = new EmailOtpValidationRequest(EMAIL, OTP, ACCOUNT_ID);
    }

    @Test
    void sendEmailOtp_WithValidRequest_ShouldReturnSuccess() {
        // Given
        when(emailOtpService.sendEmailOtp(eq(ACCOUNT_ID), eq(EMAIL))).thenReturn("OTP sent successfully");

        // When
        String response = emailOtpRestServer.sendEmailOtp(validAuthHeader, emailOtpRequest);

        // Then
        assertThat(response).isEqualTo("OTP sent successfully");
    }

    @Test
    void sendEmailOtp_WithInvalidAuthHeader_ShouldReturnError() {
        // When
        String response = emailOtpRestServer.sendEmailOtp(INVALID_AUTH_HEADER, emailOtpRequest);

        // Then
        assertThat(response).isEqualTo("Invalid JWT: Authorization header must start with 'Bearer '");
    }

    @Test
    void sendEmailOtp_WithInvalidJWT_ShouldReturnUnauthorized() {
        // Given
        when(certValidator.validateJWT(validAuthHeader.replace("Bearer ", ""))).thenReturn(false);

        // When
        String response = emailOtpRestServer.sendEmailOtp(validAuthHeader, emailOtpRequest);

        // Then
        assertThat(response).isEqualTo("Invalid or unauthorized JWT.");
    }

    @Test
    void validateEmailOtp_WithValidRequest_ShouldReturnSuccess() {
        // Given
        when(emailOtpService.validateEmailOtp(eq(EMAIL), eq(OTP), eq(ACCOUNT_ID))).thenReturn("OTP validated successfully");

        // When
        String response = emailOtpRestServer.validateEmailOtp(validValidateEmailOtpAuthHeader, emailOtpValidationRequest);

        // Then
        assertThat(response).isEqualTo("OTP validated successfully");
    }

    @Test
    void validateEmailOtp_WithInvalidAuthHeader_ShouldReturnError() {
        // When
        String response = emailOtpRestServer.validateEmailOtp(INVALID_AUTH_HEADER, emailOtpValidationRequest);

        // Then
        assertThat(response).isEqualTo("Invalid JWT: Authorization header must start with 'Bearer '");
    }

    @Test
    void validateEmailOtp_WithInvalidJWT_ShouldReturnUnauthorized() {
        // Given
        when(certValidator.validateJWT(validValidateEmailOtpAuthHeader.replace("Bearer ", ""))).thenReturn(false);

        // When
        String response = emailOtpRestServer.validateEmailOtp(validValidateEmailOtpAuthHeader, emailOtpValidationRequest);

        // Then
        assertThat(response).isEqualTo("Invalid or unauthorized JWT.");
    }
} 