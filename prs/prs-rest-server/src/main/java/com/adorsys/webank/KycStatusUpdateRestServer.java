package com.adorsys.webank;

import com.adorsys.webank.dto.KycStatusUpdateDto;
import com.adorsys.webank.dto.response.KycStatusUpdateResponse;
import com.adorsys.webank.service.KycStatusUpdateServiceApi;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class KycStatusUpdateRestServer implements KycStatusUpdateRestApi {

    private static final Logger log = LoggerFactory.getLogger(KycStatusUpdateRestServer.class);
    private final KycStatusUpdateServiceApi kycStatusUpdateServiceApi;

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public ResponseEntity<KycStatusUpdateResponse> updateKycStatus(KycStatusUpdateDto kycStatusUpdateDto) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC status update request for status: {} [correlationId={}]", kycStatusUpdateDto.getStatus(), correlationId);
        
        log.debug("Updating KYC status to {} for account ID: {} [correlationId={}]",
                kycStatusUpdateDto.getStatus(), maskAccountId(kycStatusUpdateDto.getAccountId()), correlationId);
        
        try {
            KycStatusUpdateResponse response = kycStatusUpdateServiceApi.updateKycStatus(
                    kycStatusUpdateDto.getAccountId(),
                    kycStatusUpdateDto.getStatus(),
                    kycStatusUpdateDto.getIdNumber(),
                    kycStatusUpdateDto.getExpiryDate(),
                    kycStatusUpdateDto.getRejectionReason()
            );
            
            log.info("KYC status update completed successfully [correlationId={}]", correlationId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating KYC status [correlationId={}]", correlationId, e);
            KycStatusUpdateResponse errorResponse = KycStatusUpdateResponse.builder()
                    .success(false)
                    .message("Failed to update KYC status: " + e.getMessage())
                    .status(kycStatusUpdateDto.getStatus())
                    .accountId(kycStatusUpdateDto.getAccountId())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Masks an account ID for logging purposes
     */
    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() < 5) {
            return "********";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
}
