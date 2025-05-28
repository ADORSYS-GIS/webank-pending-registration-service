package com.adorsys.webank;

import com.adorsys.webank.dto.*;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.KycServiceApi;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycRestServerTest {

    private static final String INVALID_AUTH_HEADER = "InvalidHeader";
    private static final String ACCOUNT_ID = "ACC123";
    private static final String ID_NUMBER = "ID123";
    private static final String EXPIRY_DATE = "2025-12-31";
    private static final String LOCATION = "Test Location";
    private static final String EMAIL = "test@example.com";
    private static final String FRONT_ID = "front123";
    private static final String BACK_ID = "back123";
    private static final String SELFIE_ID = "selfie123";
    private static final String TAX_ID = "tax123";
    private static final String DOCUMENT_UNIQUE_ID = "doc123";
    private static final String REJECTION_REASON = "Test rejection";

    @Mock
    private KycServiceApi kycServiceApi;

    @Mock
    private CertValidator certValidator;

    @InjectMocks
    private KycRestServer kycRestServer;

    private String validAuthHeader;
    private String validSendKyclocationAuthHeader;
    private String validSendKycEmailAuthHeader;
    private String validSendKycDocumentAuthHeader;
    private String validGetPendingKycRecordsAuthHeader;
    private String validFindByDocumentUniqueIdAuthHeader;

    @BeforeEach
    void setUp() throws Exception {
        // Generate EC key pair
        ECKey ecKey = new ECKeyGenerator(Curve.P_256).generate();

        // Create JWT with JWK in header
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
            .jwk(ecKey.toPublicJWK())
            .build();

        // Create hash for sendKycinfo using idNumber, expiryDate, and accountId
        String sendKycinfoHash = JwtValidator.hashPayload(ID_NUMBER + EXPIRY_DATE + ACCOUNT_ID);

        JWTClaimsSet sendKycinfoClaimsSet = new JWTClaimsSet.Builder()
            .subject("1234567890")
            .claim("name", "John Doe")
            .claim("iat", 1516239022)
            .claim("hash", sendKycinfoHash)
            .build();

        SignedJWT sendKycinfoJWT = new SignedJWT(header, sendKycinfoClaimsSet);
        sendKycinfoJWT.sign(new ECDSASigner(ecKey));

        // Create hash for sendKyclocation using location and accountId
        String sendKyclocationHash = JwtValidator.hashPayload(LOCATION + ACCOUNT_ID);

        JWTClaimsSet sendKyclocationClaimsSet = new JWTClaimsSet.Builder()
            .subject("1234567890")
            .claim("name", "John Doe")
            .claim("iat", 1516239022)
            .claim("hash", sendKyclocationHash)
            .build();

        SignedJWT sendKyclocationJWT = new SignedJWT(header, sendKyclocationClaimsSet);
        sendKyclocationJWT.sign(new ECDSASigner(ecKey));

        // Create hash for sendKycEmail using empty string (no parameters)
        String sendKycEmailHash = JwtValidator.hashPayload("");

        JWTClaimsSet sendKycEmailClaimsSet = new JWTClaimsSet.Builder()
            .subject("1234567890")
            .claim("name", "John Doe")
            .claim("iat", 1516239022)
            .claim("hash", sendKycEmailHash)
            .build();

        SignedJWT sendKycEmailJWT = new SignedJWT(header, sendKycEmailClaimsSet);
        sendKycEmailJWT.sign(new ECDSASigner(ecKey));

        // Create hash for sendKycDocument using frontId, backId, selfieId, taxId, and accountId
        String sendKycDocumentHash = JwtValidator.hashPayload(FRONT_ID + BACK_ID + SELFIE_ID + TAX_ID + ACCOUNT_ID);

        JWTClaimsSet sendKycDocumentClaimsSet = new JWTClaimsSet.Builder()
            .subject("1234567890")
            .claim("name", "John Doe")
            .claim("iat", 1516239022)
            .claim("hash", sendKycDocumentHash)
            .build();

        SignedJWT sendKycDocumentJWT = new SignedJWT(header, sendKycDocumentClaimsSet);
        sendKycDocumentJWT.sign(new ECDSASigner(ecKey));

        // Create hash for getPendingKycRecords using empty string (no parameters)
        String getPendingKycRecordsHash = JwtValidator.hashPayload("");

        JWTClaimsSet getPendingKycRecordsClaimsSet = new JWTClaimsSet.Builder()
            .subject("1234567890")
            .claim("name", "John Doe")
            .claim("iat", 1516239022)
            .claim("hash", getPendingKycRecordsHash)
            .build();

        SignedJWT getPendingKycRecordsJWT = new SignedJWT(header, getPendingKycRecordsClaimsSet);
        getPendingKycRecordsJWT.sign(new ECDSASigner(ecKey));

        // Create hash for findByDocumentUniqueId using documentUniqueId
        String findByDocumentUniqueIdHash = JwtValidator.hashPayload(DOCUMENT_UNIQUE_ID);

        JWTClaimsSet findByDocumentUniqueIdClaimsSet = new JWTClaimsSet.Builder()
            .subject("1234567890")
            .claim("name", "John Doe")
            .claim("iat", 1516239022)
            .claim("hash", findByDocumentUniqueIdHash)
            .build();

        SignedJWT findByDocumentUniqueIdJWT = new SignedJWT(header, findByDocumentUniqueIdClaimsSet);
        findByDocumentUniqueIdJWT.sign(new ECDSASigner(ecKey));

        validAuthHeader = "Bearer " + sendKycinfoJWT.serialize();
        validSendKyclocationAuthHeader = "Bearer " + sendKyclocationJWT.serialize();
        validSendKycEmailAuthHeader = "Bearer " + sendKycEmailJWT.serialize();
        validSendKycDocumentAuthHeader = "Bearer " + sendKycDocumentJWT.serialize();
        validGetPendingKycRecordsAuthHeader = "Bearer " + getPendingKycRecordsJWT.serialize();
        validFindByDocumentUniqueIdAuthHeader = "Bearer " + findByDocumentUniqueIdJWT.serialize();

        // Use lenient() for stubs that might not be used in all tests
        lenient().when(certValidator.validateJWT(validAuthHeader.replace("Bearer ", ""))).thenReturn(true);
        lenient().when(certValidator.validateJWT(validSendKyclocationAuthHeader.replace("Bearer ", ""))).thenReturn(true);
        lenient().when(certValidator.validateJWT(validSendKycEmailAuthHeader.replace("Bearer ", ""))).thenReturn(true);
        lenient().when(certValidator.validateJWT(validSendKycDocumentAuthHeader.replace("Bearer ", ""))).thenReturn(true);
        lenient().when(certValidator.validateJWT(validGetPendingKycRecordsAuthHeader.replace("Bearer ", ""))).thenReturn(true);
        lenient().when(certValidator.validateJWT(validFindByDocumentUniqueIdAuthHeader.replace("Bearer ", ""))).thenReturn(true);
        lenient().when(certValidator.validateJWT(INVALID_AUTH_HEADER)).thenReturn(false);
    }

    @Test
    void sendKycinfo_WithValidRequest_ShouldReturnSuccess() {
        // Given
        KycInfoRequest request = new KycInfoRequest(ID_NUMBER, EXPIRY_DATE, ACCOUNT_ID, REJECTION_REASON);
        when(kycServiceApi.sendKycInfo(eq(ACCOUNT_ID), any(KycInfoRequest.class))).thenReturn("KYC info sent successfully");

        // When
        String response = kycRestServer.sendKycinfo(validAuthHeader, request);

        // Then
        assertThat(response).isEqualTo("KYC info sent successfully");
    }

    @Test
    void sendKycinfo_WithInvalidAuthHeader_ShouldReturnError() {
        // Given
        KycInfoRequest request = new KycInfoRequest(ID_NUMBER, EXPIRY_DATE, ACCOUNT_ID, REJECTION_REASON);

        // When
        String response = kycRestServer.sendKycinfo(INVALID_AUTH_HEADER, request);

        // Then
        assertThat(response).contains("Invalid JWT");
    }

    @Test
    void sendKyclocation_WithValidRequest_ShouldReturnSuccess() {
        // Given
        KycLocationRequest request = new KycLocationRequest(LOCATION, ACCOUNT_ID);
        when(kycServiceApi.sendKycLocation(any(KycLocationRequest.class))).thenReturn("Location sent successfully");

        // When
        String response = kycRestServer.sendKyclocation(validSendKyclocationAuthHeader, request);

        // Then
        assertThat(response).isEqualTo("Location sent successfully");
    }

    @Test
    void sendKycEmail_WithValidRequest_ShouldReturnSuccess() {
        // Given
        KycEmailRequest request = new KycEmailRequest(EMAIL, ACCOUNT_ID);
        when(kycServiceApi.sendKycEmail(any(KycEmailRequest.class))).thenReturn("Email sent successfully");

        // When
        String response = kycRestServer.sendKycEmail(validSendKycEmailAuthHeader, request);

        // Then
        assertThat(response).isEqualTo("Email sent successfully");
    }

    @Test
    void sendKycDocument_WithValidRequest_ShouldReturnSuccess() {
        // Given
        KycDocumentRequest request = new KycDocumentRequest(FRONT_ID, BACK_ID, TAX_ID, SELFIE_ID, ACCOUNT_ID);
        when(kycServiceApi.sendKycDocument(eq(ACCOUNT_ID), any(KycDocumentRequest.class)))
            .thenReturn("Documents sent successfully");

        // When
        String response = kycRestServer.sendKycDocument(validSendKycDocumentAuthHeader, request);

        // Then
        assertThat(response).isEqualTo("Documents sent successfully");
    }

    @Test
    void getPendingKycRecords_WithValidRequest_ShouldReturnRecords() {
        // Given
        List<UserInfoResponse> expectedResponses = Arrays.asList(
            new UserInfoResponse(),
            new UserInfoResponse()
        );
        when(kycServiceApi.getPendingKycRecords()).thenReturn(expectedResponses);

        // When
        List<UserInfoResponse> response = kycRestServer.getPendingKycRecords(validGetPendingKycRecordsAuthHeader);

        // Then
        assertThat(response).hasSize(2);
    }

    @Test
    void getPendingKycRecords_WithInvalidAuthHeader_ShouldThrowException() {
        // When/Then
        assertThrows(IllegalArgumentException.class,
            () -> kycRestServer.getPendingKycRecords(INVALID_AUTH_HEADER));
    }

    @Test
    void findByDocumentUniqueId_WithValidRequest_ShouldReturnRecords() {
        // Given
        List<UserInfoResponse> expectedResponses = Arrays.asList(
            new UserInfoResponse(),
            new UserInfoResponse()
        );
        when(kycServiceApi.findByDocumentUniqueId(DOCUMENT_UNIQUE_ID)).thenReturn(expectedResponses);

        // When
        List<UserInfoResponse> response = kycRestServer.findByDocumentUniqueId(validFindByDocumentUniqueIdAuthHeader, DOCUMENT_UNIQUE_ID);

        // Then
        assertThat(response).hasSize(2);
    }

    @Test
    void findByDocumentUniqueId_WithInvalidAuthHeader_ShouldThrowException() {
        // When/Then
        assertThrows(IllegalArgumentException.class,
            () -> kycRestServer.findByDocumentUniqueId(INVALID_AUTH_HEADER, DOCUMENT_UNIQUE_ID));
    }
} 