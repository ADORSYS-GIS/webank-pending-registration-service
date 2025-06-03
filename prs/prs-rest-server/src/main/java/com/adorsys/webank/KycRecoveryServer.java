package com.adorsys.webank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RestController;
import com.adorsys.webank.service.KycRecoveryServiceApi;
import com.adorsys.webank.dto.KycInfoRequest;

@RestController
public class KycRecoveryServer implements KycRecoveryRestApi {

    private static final Logger log = LoggerFactory.getLogger(KycRecoveryServer.class);
    private final KycRecoveryServiceApi kycRecoveryServiceApi;

    public KycRecoveryServer(KycRecoveryServiceApi kycRecoveryServiceApi) {
        this.kycRecoveryServiceApi = kycRecoveryServiceApi;
    }

    @Override
    public String verifyKycRecoveryFields(String authorizationHeader, String accountId, KycInfoRequest kycInfoRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC recovery fields verification request [correlationId={}]", correlationId);
        
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Unauthorized access attempt - invalid authorization header [correlationId={}]", correlationId);
            throw new IllegalArgumentException("Unauthorized or invalid JWT.");
        }
        
        log.debug("Verifying KYC recovery fields for account [correlationId={}]", correlationId);
        String result = kycRecoveryServiceApi.verifyKycRecoveryFields(
                accountId,
                kycInfoRequest.getIdNumber(),
                kycInfoRequest.getExpiryDate()
        );
        
        log.info("KYC recovery fields verification completed [correlationId={}]", correlationId);
        return result;
    }
}