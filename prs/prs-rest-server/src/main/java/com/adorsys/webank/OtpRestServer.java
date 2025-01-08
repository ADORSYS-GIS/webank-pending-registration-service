package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.service.OtpServiceApi;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OtpRestServer implements OtpRestApi {
    private final OtpServiceApi otpService;

    public OtpRestServer(OtpServiceApi otpService) {

        this.otpService = otpService;
    }

    @Override
    public String sendOtp(OtpRequest request) {
        return otpService.sendOtp(request.getPhoneNumber());
    }

    @Override
    public boolean validateOtp(OtpValidationRequest request) {
        return otpService.validateOtp(request.getPhoneNumber(), request.getOtp());
    }
}
