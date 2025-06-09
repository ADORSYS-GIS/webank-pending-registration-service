package com.adorsys.webank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.service.KycStatusUpdateServiceApi;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class KycStatusUpdateRestServer implements KycStatusUpdateRestApi {

    private static final Logger log = LoggerFactory.getLogger(KycStatusUpdateRestServer.class);
    private final KycStatusUpdateServiceApi kycStatusUpdateServiceApi;

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String updateKycStatus(String authorizationHeader, String accountId, String status, KycInfoRequest kycInfoRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC status update request for status: {} [correlationId={}]", status, correlationId);
        
        log.debug("Updating KYC status to {} for account ID: {} [correlationId={}]", 
                status, maskAccountId(accountId), correlationId);
        
        String result = kycStatusUpdateServiceApi.updateKycStatus(
            accountId, 
            status, 
            kycInfoRequest.getIdNumber(),  
            kycInfoRequest.getExpiryDate(), 
            kycInfoRequest.getRejectionReason()
        );
        
        log.info("KYC status update completed [correlationId={}]", correlationId);
        return result;
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
