package com.adorsys.webank.serviceimpl;

import com.adorsys.error.ValidationException;
import com.adorsys.webank.config.CertGeneratorHelper;
import com.adorsys.webank.config.JwtValidator;
import com.adorsys.webank.config.SecurityUtils;
import com.adorsys.webank.dto.AccountRecoveryResponse;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountRecoveryValidationRequestServiceImplTest {

    @Mock
    private CertGeneratorHelper certGeneratorHelper;

    private AccountRecoveryValidationRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountRecoveryValidationRequestServiceImpl(certGeneratorHelper);
    }

    @Test
    void testProcessRecovery_Success() throws Exception {
        // Given
        String newAccountId = "acc123";
        String oldAccountId = "old456";
        String jwtToken = "mock-jwt";
        String recoveryJwt = "mock-recovery-jwt";
        String generatedCertificate = "mock-cert";

        ECKey mockEcKey = mock(ECKey.class);
        when(mockEcKey.toJSONString()).thenReturn("mock-jwk-json");

        SignedJWT signedJWT = mock(SignedJWT.class);
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);

        try (
                MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class);
                MockedStatic<JwtValidator> jwtValidatorMock = mockStatic(JwtValidator.class);
                MockedStatic<SignedJWT> signedJwtStatic = mockStatic(SignedJWT.class)
        ) {
            // Mock static methods
            securityUtilsMock.when(SecurityUtils::getCurrentUserJWT).thenReturn(Optional.of(jwtToken));
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(mockEcKey);
            jwtValidatorMock.when(() -> JwtValidator.extractClaim(jwtToken, "recoveryJwt")).thenReturn(recoveryJwt);
            signedJwtStatic.when(() -> SignedJWT.parse(recoveryJwt)).thenReturn(signedJWT);

            // Mock SignedJWT & claims
            when(signedJWT.getJWTClaimsSet()).thenReturn(claimsSet);
            when(claimsSet.getStringClaim("newAccountId")).thenReturn(newAccountId);
            when(claimsSet.getStringClaim("oldAccountId")).thenReturn(oldAccountId);

            // Mock certificate generation
            when(certGeneratorHelper.generateCertificate("mock-jwk-json")).thenReturn(generatedCertificate);

            // Act
            AccountRecoveryResponse response = service.processRecovery(newAccountId);

            // Assert
            assertNotNull(response);
            assertEquals("Account recovery successful", response.getMessage());
        }
    }

    @Test
    void testProcessRecovery_JwtTokenMissing_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserJWT).thenReturn(Optional.empty());

            Exception ex = assertThrows(IllegalStateException.class, () -> {
                service.processRecovery("acc123");
            });

            assertEquals("No JWT token found in security context", ex.getMessage());
        }
    }

    @Test
    void testProcessRecovery_ClaimingAccountIdMismatch_ThrowsValidationException() throws Exception {
        String newAccountId = "acc123";
        String recoveryJwt = "mock-recovery-jwt";
        String jwtToken = "mock-jwt";

        SignedJWT signedJWT = mock(SignedJWT.class);
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);

        try (
                MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class);
                MockedStatic<JwtValidator> jwtValidatorMock = mockStatic(JwtValidator.class);
                MockedStatic<SignedJWT> signedJwtStatic = mockStatic(SignedJWT.class)
        ) {
            // Setup static mocks
            securityUtilsMock.when(SecurityUtils::getCurrentUserJWT).thenReturn(Optional.of(jwtToken));
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(mock(ECKey.class));
            jwtValidatorMock.when(() -> JwtValidator.extractClaim(jwtToken, "recoveryJwt")).thenReturn(recoveryJwt);
            signedJwtStatic.when(() -> SignedJWT.parse(recoveryJwt)).thenReturn(signedJWT);

            // Simulate mismatching newAccountId
            when(signedJWT.getJWTClaimsSet()).thenReturn(claimsSet);
            when(claimsSet.getStringClaim("newAccountId")).thenReturn("wrong-id");

            // Act & Assert
            assertThrows(ValidationException.class, () -> {
                service.processRecovery(newAccountId);
            });
        }
    }

    @Test
    void testProcessRecovery_InvalidRecoveryJwtFormat_ThrowsValidationException() throws Exception {
        String newAccountId = "acc123";
        String jwtToken = "mock-jwt";
        String recoveryJwt = "bad-jwt";

        try (
                MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class);
                MockedStatic<JwtValidator> jwtValidatorMock = mockStatic(JwtValidator.class);
                MockedStatic<SignedJWT> signedJwtStatic = mockStatic(SignedJWT.class)
        ) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserJWT).thenReturn(Optional.of(jwtToken));
            securityUtilsMock.when(SecurityUtils::extractDeviceJwkFromContext).thenReturn(mock(ECKey.class));
            jwtValidatorMock.when(() -> JwtValidator.extractClaim(jwtToken, "recoveryJwt")).thenReturn(recoveryJwt);

            // Simulate JWT parse failure
            signedJwtStatic.when(() -> SignedJWT.parse(recoveryJwt))
                    .thenThrow(new ParseException("Invalid Recovery JWT", 0));

            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class, () -> {
                service.processRecovery(newAccountId);
            });

            assertEquals("Invalid RecoveryJWT format", exception.getMessage());
        }
    }
}
