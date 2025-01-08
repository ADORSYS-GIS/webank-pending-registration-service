package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.service.OtpServiceApi;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class OtpServiceImpl implements OtpServiceApi {

    @Override
    public String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int otp = 1000 + secureRandom.nextInt(9000);
        return String.valueOf(otp);
    }

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
