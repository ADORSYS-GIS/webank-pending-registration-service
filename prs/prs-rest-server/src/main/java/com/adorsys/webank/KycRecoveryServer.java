package com.adorsys.webank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RestController;
import com.adorsys.webank.service.KycRecoveryServiceApi;
import com.adorsys.webank.dto.KycInfoRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class KycRecoveryServer implements KycRecoveryRestApi {

    private static final Logger log = LoggerFactory.getLogger(KycRecoveryServer.class);
    private final KycRecoveryServiceApi kycRecoveryServiceApi;
    
    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String verifyKycRecoveryFields(String authorizationHeader, String accountId, KycInfoRequest kycInfoRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC recovery fields verification request [correlationId={}]", correlationId);
        
        log.debug("Verifying KYC recovery fields for account ID: {} [correlationId={}]", 
                maskAccountId(accountId), correlationId);
        
        String result = kycRecoveryServiceApi.verifyKycRecoveryFields(
                accountId,
                kycInfoRequest.getIdNumber(),
                kycInfoRequest.getExpiryDate()
        );
        
        log.info("KYC recovery fields verification completed [correlationId={}]", correlationId);
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