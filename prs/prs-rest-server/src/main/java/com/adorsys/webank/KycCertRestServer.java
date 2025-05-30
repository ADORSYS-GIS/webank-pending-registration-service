package com.adorsys.webank;
import com.adorsys.webank.service.KycCertServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import java.text.ParseException;

@RestController
public class KycCertRestServer implements KycCertRestApi {

    private static final Logger log = LoggerFactory.getLogger(KycCertRestServer.class);
    private final KycCertServiceApi kycCertServiceApi;

    public KycCertRestServer(KycCertServiceApi kycCertServiceApi) {
        this.kycCertServiceApi = kycCertServiceApi;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String getCert(String authorizationHeader, String accountId) throws ParseException {

        log.info("Getting KYC certificate for accountId: {}", accountId);

        // Retrieve and return the KYC certificate
        return kycCertServiceApi.getCert(accountId);
    }
}
