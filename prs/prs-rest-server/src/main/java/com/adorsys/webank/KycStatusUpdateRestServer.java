package com.adorsys.webank;

import com.adorsys.webank.dto.KycStatusUpdateDto;
import com.adorsys.webank.dto.response.KycStatusUpdateResponse;
import com.adorsys.webank.service.KycStatusUpdateServiceApi;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST server implementation for KYC status update operations.
 * Handles incoming requests, performs authorization, and delegates to the service layer.
 */
@RestController
@RequiredArgsConstructor
public class KycStatusUpdateRestServer implements KycStatusUpdateRestApi {

    private static final Logger log = LoggerFactory.getLogger(KycStatusUpdateRestServer.class);
    private static final String LOG_FORMAT = "{} [correlationId={}]";
    
    private final KycStatusUpdateServiceApi kycStatusUpdateServiceApi;

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public ResponseEntity<KycStatusUpdateResponse> updateKycStatus(KycStatusUpdateDto kycStatusUpdateDto) {
        String correlationId = MDC.get("correlationId");
        String maskedAccountId = maskAccountId(kycStatusUpdateDto.getAccountId());
        
        log.info("Received KYC status update request - Status: {}, Account: {} [correlationId={}]", 
                kycStatusUpdateDto.getStatus(), maskedAccountId, correlationId);
        
        try {
            log.debug("Processing KYC status update - Status: {}, Account: {} [correlationId={}]",
                    kycStatusUpdateDto.getStatus(), maskedAccountId, correlationId);
            
            KycStatusUpdateResponse response = kycStatusUpdateServiceApi.updateKycStatus(
                    kycStatusUpdateDto.getAccountId(),
                    kycStatusUpdateDto.getStatus(),
                    kycStatusUpdateDto.getIdNumber(),
                    kycStatusUpdateDto.getExpiryDate(),
                    kycStatusUpdateDto.getRejectionReason()
            );
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : 
                             response.getMessage().contains("not found") ? HttpStatus.NOT_FOUND : 
                             HttpStatus.BAD_REQUEST;
            
            log.info("KYC status update completed - Status: {}, Account: {} [correlationId={}]", 
                    response.getStatus(), maskedAccountId, correlationId);
                    
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error processing KYC status update for account: {} [correlationId={}]", 
                    maskedAccountId, correlationId, e);
                    
            KycStatusUpdateResponse errorResponse = KycStatusUpdateResponse.error(
                    "An unexpected error occurred while processing your request: " + e.getMessage(),
                    kycStatusUpdateDto.getAccountId(),
                    kycStatusUpdateDto.getIdNumber()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Masks sensitive account ID information for logging purposes.
     * 
     * @param accountId The account ID to mask
     * @return A masked version of the account ID showing only first 2 and last 2 characters
     */
    private String maskAccountId(String accountId) {
        if (accountId == null) {
            return "null";
        }
        if (accountId.length() <= 4) {
            return "****";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
}
