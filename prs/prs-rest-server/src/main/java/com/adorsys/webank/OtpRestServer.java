package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.service.OtpServiceApi;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import java.text.ParseException;

@RestController
public class OtpRestServer implements OtpRestApi {
    private final OtpServiceApi otpService;

    public OtpRestServer(OtpServiceApi otpService) {
        this.otpService = otpService;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendOtp(String authorizationHeader, OtpRequest request) throws ParseException {
        return otpService.sendOtp(request.getPhoneNumber());
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String validateOtp(String authorizationHeader, OtpValidationRequest request) throws ParseException {
        return otpService.validateOtp(request.getPhoneNumber(), request.getOtpInput());
    }

}