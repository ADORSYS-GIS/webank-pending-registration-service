package com.adorsys.webank;

import com.adorsys.webank.service.KycStatusUpdateServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RestController;
import com.adorsys.webank.dto.KycInfoRequest;

@RestController
public class KycStatusUpdateRestServer implements KycStatusUpdateRestApi {

    private static final Logger log = LoggerFactory.getLogger(KycStatusUpdateRestServer.class);
    private final KycStatusUpdateServiceApi kycStatusUpdateServiceApi;

    public KycStatusUpdateRestServer(KycStatusUpdateServiceApi kycStatusUpdateServiceApi) {
        this.kycStatusUpdateServiceApi = kycStatusUpdateServiceApi;
    }

    @Override
    public String updateKycStatus(String authorizationHeader, String accountId, String status, KycInfoRequest kycInfoRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC status update request for status: {} [correlationId={}]", status, correlationId);
        
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ") ) {
            log.warn("Unauthorized access attempt - invalid authorization header [correlationId={}]", correlationId);
            throw new IllegalArgumentException("Unauthorized or invalid JWT.");
        }
        
        log.debug("Updating KYC status to {} [correlationId={}]", status, correlationId);
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
}
