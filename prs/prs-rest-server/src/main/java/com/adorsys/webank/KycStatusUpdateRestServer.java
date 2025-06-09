package com.adorsys.webank;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.service.KycStatusUpdateServiceApi;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class KycStatusUpdateRestServer implements KycStatusUpdateRestApi {

    private final KycStatusUpdateServiceApi kyctatusUpdateServiceApi;


    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String updateKycStatus(String authorizationHeader, String accountId, String status, KycInfoRequest kycInfoRequest) {
        return kyctatusUpdateServiceApi.updateKycStatus(accountId, status, kycInfoRequest.getIdNumber(),  kycInfoRequest.getExpiryDate(), kycInfoRequest.getRejectionReason());
    }
}
