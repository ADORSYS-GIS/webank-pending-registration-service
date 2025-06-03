package com.adorsys.webank;
import com.adorsys.webank.service.KycCertServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import com.adorsys.webank.exceptions.InvalidDateException;
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

        log.info("Getting KYC certificate for accountId: {}", accountId);

        // Retrieve and return the KYC certificate
        return kycCertServiceApi.getCert(accountId);
    }
}
