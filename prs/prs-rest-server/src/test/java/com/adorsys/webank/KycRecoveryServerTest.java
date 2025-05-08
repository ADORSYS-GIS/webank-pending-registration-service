package com.adorsys.webank;

import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.service.KycRecoveryServiceApi;
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
class KycRecoveryServerTest {

    private static final String VALID_AUTH_HEADER = "Bearer valid.jwt.token";
    private static final String INVALID_AUTH_HEADER = "InvalidHeader";
    private static final String ACCOUNT_ID = "ACC123";
    private static final String ID_NUMBER = "ID123";
    private static final String EXPIRY_DATE = "2025-12-31";
    private static final String REJECTION_REASON = "Invalid document";

    @Mock
    private KycRecoveryServiceApi kycRecoveryServiceApi;

    @InjectMocks
    private KycRecoveryServer kycRecoveryServer;

    private KycInfoRequest kycInfoRequest;

    @BeforeEach
    void setUp() {
        kycInfoRequest = new KycInfoRequest(ID_NUMBER, EXPIRY_DATE, ACCOUNT_ID, REJECTION_REASON);
    }

    @Test
    void verifyKycRecoveryFields_WithValidRequest_ShouldReturnSuccess() {
        // Given
        when(kycRecoveryServiceApi.verifyKycRecoveryFields(
            anyString(), anyString(), anyString()))
            .thenReturn("KYC recovery fields verified successfully");

        // When
        String response = kycRecoveryServer.verifyKycRecoveryFields(
            VALID_AUTH_HEADER, ACCOUNT_ID, kycInfoRequest);

        // Then
        assertThat(response).isEqualTo("KYC recovery fields verified successfully");
    }

    @Test
    void verifyKycRecoveryFields_WithInvalidAuthHeader_ShouldThrowException() {
        // When/Then
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> kycRecoveryServer.verifyKycRecoveryFields(
                INVALID_AUTH_HEADER, ACCOUNT_ID, kycInfoRequest)
        );
    }
} 