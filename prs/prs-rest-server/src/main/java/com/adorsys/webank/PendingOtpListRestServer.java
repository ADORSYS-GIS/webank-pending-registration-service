package com.adorsys.webank;

import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.service.PendingOtpServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PendingOtpListRestServer implements PendingOtpListRestApi {

    private static final Logger log = LoggerFactory.getLogger(PendingOtpListRestServer.class);
    private final PendingOtpServiceApi pendingOtpServiceApi;

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public List<PendingOtpDto> getPendingOtps(String authorizationHeader) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to fetch pending OTPs [correlationId={}]", correlationId);
        
        // Delegate to the service to retrieve pending OTP records
        log.debug("Fetching pending OTP entries [correlationId={}]", correlationId);
        List<PendingOtpDto> pendingOtps = pendingOtpServiceApi.fetchPendingOtpEntries();
        log.info("Retrieved {} pending OTP entries [correlationId={}]", 
                pendingOtps.size(), correlationId);
        
        return pendingOtps;
    }
}
