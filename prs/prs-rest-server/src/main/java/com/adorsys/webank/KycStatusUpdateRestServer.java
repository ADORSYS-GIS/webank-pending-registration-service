package com.adorsys.webank;

import com.adorsys.webank.service.KycStatusUpdateServiceApi;
import org.springframework.web.bind.annotation.RestController;
import com.adorsys.webank.dto.KycInfoRequest;

@RestController
public class KycStatusUpdateRestServer implements KycStatusUpdateRestApi {

    private final KycStatusUpdateServiceApi kyctatusUpdateServiceApi;

    public KycStatusUpdateRestServer(KycStatusUpdateServiceApi kyctatusUpdateServiceApi) {
        this.kyctatusUpdateServiceApi = kyctatusUpdateServiceApi;
    }

    @Override
    public String updateKycStatus(String authorizationHeader, String accountId, String status, KycInfoRequest kycInfoRequest) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ") ) {
            throw new IllegalArgumentException("Unauthorized or invalid JWT.");
        }
        return kyctatusUpdateServiceApi.updateKycStatus(accountId, status, kycInfoRequest.getIdNumber(),  kycInfoRequest.getExpiryDate());
    }
}
