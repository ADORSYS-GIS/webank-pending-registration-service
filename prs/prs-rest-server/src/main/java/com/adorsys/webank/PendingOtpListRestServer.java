package com.adorsys.webank;

import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.PendingOtpServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
        String correlationId = MDC.get("correlationId");
        log.info("Received request to fetch pending OTPs [correlationId={}]", correlationId);
        
        String jwtToken;
        try {
            // Extract the JWT token from the Authorization header
            log.debug("Extracting and validating JWT token [correlationId={}]", correlationId);
            jwtToken = extractJwtFromHeader(authorizationHeader);
            JwtValidator.validateAndExtract(jwtToken);
            log.debug("JWT token validation successful [correlationId={}]", correlationId);
        } catch (Exception e) {
            log.error("JWT validation failed [correlationId={}]", correlationId, e);
            throw new IllegalArgumentException("An error occurred");
        }
        
        // Delegate to the service to retrieve pending OTP records.
        log.info("Fetching pending OTP entries [correlationId={}]", correlationId);
        List<PendingOtpDto> pendingOtps = pendingOtpServiceApi.fetchPendingOtpEntries();
        log.info("Retrieved {} pending OTP entries [correlationId={}]", 
                pendingOtps.size(), correlationId);
        
        return pendingOtps;
    }
    
    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Invalid authorization header format");
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}
