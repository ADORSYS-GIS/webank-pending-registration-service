package com.adorsys.webank;

import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.service.KycStatusUpdateServiceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycStatusUpdateRestServerTest {

    private static final String VALID_AUTH_HEADER = "Bearer valid.jwt.token";
    private static final String INVALID_AUTH_HEADER = "InvalidHeader";
    private static final String ACCOUNT_ID = "ACC123";
    private static final String STATUS = "APPROVED";
    private static final String ID_NUMBER = "ID123";
    private static final String EXPIRY_DATE = "2025-12-31";
    private static final String REJECTION_REASON = "Invalid document";

    @Mock
    private KycStatusUpdateServiceApi kycStatusUpdateServiceApi;

    @InjectMocks
    private KycStatusUpdateRestServer kycStatusUpdateRestServer;

    private KycInfoRequest kycInfoRequest;

    @BeforeEach
    void setUp() {
        kycInfoRequest = new KycInfoRequest(ID_NUMBER, EXPIRY_DATE, ACCOUNT_ID, REJECTION_REASON);
    }

    @Test
    void updateKycStatus_WithValidRequest_ShouldReturnSuccess() {
        // Given
        when(kycStatusUpdateServiceApi.updateKycStatus(
            anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn("KYC status updated successfully");

        // When
        String response = kycStatusUpdateRestServer.updateKycStatus(
            VALID_AUTH_HEADER, ACCOUNT_ID, STATUS, kycInfoRequest);

        // Then
        assertThat(response).isEqualTo("KYC status updated successfully");
    }

    @Test
    void updateKycStatus_WithInvalidAuthHeader_ShouldThrowException() {
        // When/Then
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> kycStatusUpdateRestServer.updateKycStatus(
                INVALID_AUTH_HEADER, ACCOUNT_ID, STATUS, kycInfoRequest)
        );
    }
} 