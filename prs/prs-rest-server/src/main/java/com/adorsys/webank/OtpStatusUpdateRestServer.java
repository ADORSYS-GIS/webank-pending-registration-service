package com.adorsys.webank;

import com.adorsys.webank.dto.OtpStatusUpdateRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.service.OtpStatusUpdateServiceApi;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OtpStatusUpdateRestServer implements OtpStatusUpdateRestApi {

    private final OtpStatusUpdateServiceApi otpStatusUpdateServiceApi;
    private final CertValidator certValidator;

    public OtpStatusUpdateRestServer(OtpStatusUpdateServiceApi otpStatusUpdateServiceApi, CertValidator certValidator) {
        this.otpStatusUpdateServiceApi = otpStatusUpdateServiceApi;
        this.certValidator = certValidator;
    }

    @Override
    public String updateOtpStatus(String authorizationHeader, String phoneNumber, OtpStatusUpdateRequest request) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ") ||
                !certValidator.validateJWT(authorizationHeader.substring(7))) {
            throw new IllegalArgumentException("Unauthorized or invalid JWT.");
        }
        return otpStatusUpdateServiceApi.updateOtpStatus(phoneNumber, request.getStatus());
    }
}
