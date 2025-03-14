package com.adorsys.webank;

import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.service.PendingOtpServiceApi;
import com.adorsys.webank.security.CertValidator;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PendingOtpListRestServer implements PendingOtpListRestApi {

    private final PendingOtpServiceApi pendingOtpServiceApi;
    private final CertValidator certValidator;

    // Removed the PendingOtpListRestApi parameter to break the circular dependency.
    public PendingOtpListRestServer(PendingOtpServiceApi pendingOtpServiceApi, CertValidator certValidator) {
        this.pendingOtpServiceApi = pendingOtpServiceApi;
        this.certValidator = certValidator;
    }

    @Override
    public List<PendingOtpDto> getPendingOtps(String authorizationHeader) {
        // Validate the authorization header using the CertValidator.
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ") ||
                !certValidator.validateJWT(authorizationHeader.substring(7))) {
            throw new IllegalArgumentException("Unauthorized or invalid JWT.");
        }
        // Delegate to the service to retrieve pending OTP records.
        return pendingOtpServiceApi.getPendingOtps();
    }
}
