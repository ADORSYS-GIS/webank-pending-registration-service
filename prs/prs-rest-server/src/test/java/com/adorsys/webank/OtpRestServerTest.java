package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.OtpServiceApi;
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
class OtpRestServerTest {

    private static final String INVALID_AUTH_HEADER = "InvalidHeader";
    private static final String PHONE_NUMBER = "+1234567890";
    private static final String OTP = "123456";

    @Mock
    private OtpServiceApi otpService;

    @Mock
    private CertValidator certValidator;

    @Mock
    private JWK mockPublicKey;

    @InjectMocks
    private OtpRestServer otpRestServer;

    private String validAuthHeader;
    private String validValidateOtpAuthHeader;
    private OtpRequest otpRequest;
    private OtpValidationRequest otpValidationRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Generate EC key pair
        ECKey ecKey = new ECKeyGenerator(Curve.P_256).generate();

        // Create JWT with JWK in header
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
            .jwk(ecKey.toPublicJWK())
            .build();

        // Create hash for sendOtp using only phone number
        String sendOtpHash = JwtValidator.hashPayload(PHONE_NUMBER);

        JWTClaimsSet sendOtpClaimsSet = new JWTClaimsSet.Builder()
            .subject("1234567890")
            .claim("name", "John Doe")
            .claim("iat", 1516239022)
            .claim("hash", sendOtpHash)
            .build();

        SignedJWT sendOtpJWT = new SignedJWT(header, sendOtpClaimsSet);
        sendOtpJWT.sign(new ECDSASigner(ecKey));

        // Create hash for validateOtp using phone number and OTP
        String validateOtpHash = JwtValidator.hashPayload(PHONE_NUMBER + OTP);

        JWTClaimsSet validateOtpClaimsSet = new JWTClaimsSet.Builder()
            .subject("1234567890")
            .claim("name", "John Doe")
            .claim("iat", 1516239022)
            .claim("hash", validateOtpHash)
            .build();

        SignedJWT validateOtpJWT = new SignedJWT(header, validateOtpClaimsSet);
        validateOtpJWT.sign(new ECDSASigner(ecKey));

        validAuthHeader = "Bearer " + sendOtpJWT.serialize();
        validValidateOtpAuthHeader = "Bearer " + validateOtpJWT.serialize();

        // Use lenient() for stubs that might not be used in all tests
        lenient().when(certValidator.validateJWT(validAuthHeader.replace("Bearer ", ""))).thenReturn(true);
        lenient().when(certValidator.validateJWT(validValidateOtpAuthHeader.replace("Bearer ", ""))).thenReturn(true);
        lenient().when(certValidator.validateJWT(INVALID_AUTH_HEADER)).thenReturn(false);

        otpRequest = new OtpRequest(PHONE_NUMBER);
        otpValidationRequest = new OtpValidationRequest(PHONE_NUMBER, OTP);
    }

    @Test
    void sendOtp_WithValidRequest_ShouldReturnSuccess() {
        // Given
        when(otpService.sendOtp(any(JWK.class), eq(PHONE_NUMBER))).thenReturn("OTP sent successfully");

        // When
        String response = otpRestServer.sendOtp(validAuthHeader, otpRequest);

        // Then
        assertThat(response).isEqualTo("OTP sent successfully");
    }

    @Test
    void sendOtp_WithInvalidAuthHeader_ShouldReturnError() {
        // When
        String response = otpRestServer.sendOtp(INVALID_AUTH_HEADER, otpRequest);

        // Then
        assertThat(response).isEqualTo("Invalid JWT: Authorization header must start with 'Bearer '");
    }

    @Test
    void sendOtp_WithInvalidJWT_ShouldReturnUnauthorized() {
        // Given
        when(certValidator.validateJWT(validAuthHeader.replace("Bearer ", ""))).thenReturn(false);

        // When
        String response = otpRestServer.sendOtp(validAuthHeader, otpRequest);

        // Then
        assertThat(response).isEqualTo("Invalid or unauthorized JWT.");
    }

    @Test
    void validateOtp_WithValidRequest_ShouldReturnSuccess() {
        // Given
        when(otpService.validateOtp(eq(PHONE_NUMBER), any(JWK.class), eq(OTP))).thenReturn("OTP validated successfully");

        // When
        String response = otpRestServer.validateOtp(validValidateOtpAuthHeader, otpValidationRequest);

        // Then
        assertThat(response).isEqualTo("OTP validated successfully");
    }

    @Test
    void validateOtp_WithInvalidAuthHeader_ShouldReturnError() {
        // When
        String response = otpRestServer.validateOtp(INVALID_AUTH_HEADER, otpValidationRequest);

        // Then
        assertThat(response).isEqualTo("Invalid JWT: Authorization header must start with 'Bearer '");
    }

    @Test
    void validateOtp_WithInvalidJWT_ShouldReturnUnauthorized() {
        // Given
        when(certValidator.validateJWT(validValidateOtpAuthHeader.replace("Bearer ", ""))).thenReturn(false);

        // When
        String response = otpRestServer.validateOtp(validValidateOtpAuthHeader, otpValidationRequest);

        // Then
        assertThat(response).isEqualTo("Invalid or unauthorized JWT.");
    }
} 