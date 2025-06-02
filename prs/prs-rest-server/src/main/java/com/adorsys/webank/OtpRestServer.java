package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.service.OtpServiceApi;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import com.adorsys.webank.exceptions.InvalidDateException;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OtpRestServer implements OtpRestApi {
    private final OtpServiceApi otpService;


    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendOtp(String authorizationHeader, OtpRequest request) throws InvalidDateException {
        return otpService.sendOtp(request.getPhoneNumber());
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String validateOtp(String authorizationHeader, OtpValidationRequest request) throws InvalidDateException {
        return otpService.validateOtp(request.getPhoneNumber(), request.getOtpInput());
    }

}