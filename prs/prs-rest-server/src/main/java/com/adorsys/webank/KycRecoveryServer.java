package com.adorsys.webank;
import org.springframework.web.bind.annotation.RestController;
import com.adorsys.webank.service.KycRecoveryServiceApi;
import com.adorsys.webank.dto.KycInfoRequest;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class KycRecoveryServer implements KycRecoveryRestApi {

    private final KycRecoveryServiceApi kycRecoveryServiceApi;

    public KycRecoveryServer(KycRecoveryServiceApi kycRecoveryServiceApi) {
        this.kycRecoveryServiceApi = kycRecoveryServiceApi;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String verifyKycRecoveryFields(String authorizationHeader, String accountId, KycInfoRequest kycInfoRequest) {
        return kycRecoveryServiceApi.verifyKycRecoveryFields(
                accountId,
                kycInfoRequest.getIdNumber(),
                kycInfoRequest.getExpiryDate()
        );
    }
}