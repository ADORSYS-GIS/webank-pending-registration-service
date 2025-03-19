package com.adorsys.webank;

import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.service.KycStatusUpdateServiceApi;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KycStatusUpdateRestServer implements KycStatusUpdateRestApi {

    private final KycStatusUpdateServiceApi kyctatusUpdateServiceApi;
    private final CertValidator certValidator;

    public KycStatusUpdateRestServer(KycStatusUpdateServiceApi kyctatusUpdateServiceApi, CertValidator certValidator) {
        this.kyctatusUpdateServiceApi = kyctatusUpdateServiceApi;
        this.certValidator = certValidator;
    }

    @Override
    public String updateKycStatus(String authorizationHeader, String publicKeyHash, String status) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ") ) {
            throw new IllegalArgumentException("Unauthorized or invalid JWT.");
        }
        return kyctatusUpdateServiceApi.updateKycStatus(publicKeyHash, status);
    }
}
