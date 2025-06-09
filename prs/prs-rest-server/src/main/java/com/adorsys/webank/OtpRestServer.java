package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.service.OtpServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OtpRestServer implements OtpRestApi {
    private static final Logger log = LoggerFactory.getLogger(OtpRestServer.class);
    
    private final OtpServiceApi otpService;

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendOtp(String authorizationHeader, OtpRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("Received OTP send request [correlationId={}]", correlationId);
        
        // Add user phone number to MDC (masked)
        MDC.put("phoneNumber", maskPhoneNumber(request.getPhoneNumber()));
        
        try {
            log.debug("Processing OTP send request [correlationId={}]", correlationId);
            String result = otpService.sendOtp(request.getPhoneNumber());
            log.info("OTP send operation completed successfully [correlationId={}]", correlationId);
            return result;
        } finally {
            // Remove any additional MDC values we added
            MDC.remove("phoneNumber");
        }
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String validateOtp(String authorizationHeader, OtpValidationRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("Received OTP validation request [correlationId={}]", correlationId);
        
        // Add user phone number to MDC (masked)
        MDC.put("phoneNumber", maskPhoneNumber(request.getPhoneNumber()));
        
        try {
            log.debug("Processing OTP validation request [correlationId={}]", correlationId);
            String result = otpService.validateOtp(request.getPhoneNumber(), request.getOtpInput());
            log.info("OTP validation operation completed [correlationId={}]", correlationId);
            return result;
        } finally {
            // Remove any additional MDC values we added
            MDC.remove("phoneNumber");
        }
    }
    
    /**
     * Masks a phone number for logging purposes
     * Shows only last 4 digits, rest are masked
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "********";
        }
        return "******" + phoneNumber.substring(Math.max(0, phoneNumber.length() - 4));
    }
}