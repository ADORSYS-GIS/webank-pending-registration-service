package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.service.OtpServiceApi;
import org.springframework.stereotype.Service;

@Service
public class OtpServiceImpl implements OtpServiceApi {

    @Override
    public String sendOtp(String phoneNumber) {
        // Implementation will go here
        return "OTP success";
    }

    @Override
    public boolean validateOtp(String phoneNumber, String otp) {
        // Implementation will go here
        return false;
    }
}
