package com.adorsys.webank;

import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.PendingOtpServiceApi;
import com.adorsys.error.JwtValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PendingOtpListRestServer implements PendingOtpListRestApi {

    private static final Logger log = LoggerFactory.getLogger(PendingOtpListRestServer.class);
    private final PendingOtpServiceApi pendingOtpServiceApi;

    // Removed the PendingOtpListRestApi parameter to break the circular dependency.
    public PendingOtpListRestServer(PendingOtpServiceApi pendingOtpServiceApi) {
        this.pendingOtpServiceApi = pendingOtpServiceApi;
    }

    @Override
    public List<PendingOtpDto> getPendingOtps(String authorizationHeader) {
        String jwtToken;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            JwtValidator.validateAndExtract(jwtToken);
            log.info("Success");
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }
        // Delegate to the service to retrieve pending OTP records.
        return pendingOtpServiceApi.fetchPendingOtpEntries();
    }
    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new JwtValidationException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}
