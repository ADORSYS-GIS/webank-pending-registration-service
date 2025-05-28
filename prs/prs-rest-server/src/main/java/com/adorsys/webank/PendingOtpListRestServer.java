package com.adorsys.webank;

import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.service.PendingOtpServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
public class PendingOtpListRestServer implements PendingOtpListRestApi {

    private static final Logger log = LoggerFactory.getLogger(PendingOtpListRestServer.class);
    private final PendingOtpServiceApi pendingOtpServiceApi;

    public PendingOtpListRestServer(PendingOtpServiceApi pendingOtpServiceApi) {
        this.pendingOtpServiceApi = pendingOtpServiceApi;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public List<PendingOtpDto> getPendingOtps(String authorizationHeader) {

        log.info("Fetching pending OTP entries");
        // Delegate to the service to retrieve pending OTP records.
        return pendingOtpServiceApi.fetchPendingOtpEntries();
    }
}
