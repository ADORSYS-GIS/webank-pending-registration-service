package com.adorsys.webank;
import com.adorsys.webank.service.KycCertServiceApi;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
public class KycCertRestServer implements KycCertRestApi {
    
    private final KycCertServiceApi kycCertServiceApi;

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String getCert(String authorizationHeader, String accountId) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to get KYC certificate for account: {} [correlationId={}]", 
                maskAccountId(accountId), correlationId);

        log.debug("Processing KYC certificate request [correlationId={}]", correlationId);
        String result = kycCertServiceApi.getCert(accountId);
        log.info("KYC certificate retrieval completed [correlationId={}]", correlationId);
        
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
