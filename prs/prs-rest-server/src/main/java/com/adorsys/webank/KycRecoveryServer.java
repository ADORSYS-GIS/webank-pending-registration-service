package com.adorsys.webank;
import org.springframework.web.bind.annotation.RestController;
import com.adorsys.webank.service.KycRecoveryServiceApi;
import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.error.JwtValidationException;

@RestController
public class KycRecoveryServer implements KycRecoveryRestApi {

    private final KycRecoveryServiceApi kycRecoveryServiceApi;

    public KycRecoveryServer(KycRecoveryServiceApi kycRecoveryServiceApi) {
        this.kycRecoveryServiceApi = kycRecoveryServiceApi;
    }

    @Override
    public String verifyKycRecoveryFields(String authorizationHeader, String accountId, KycInfoRequest kycInfoRequest) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new JwtValidationException("Authorization header must start with 'Bearer '");
        }
        return kycRecoveryServiceApi.verifyKycRecoveryFields(
                accountId,
                kycInfoRequest.getIdNumber(),
                kycInfoRequest.getExpiryDate()
        );
    }
}