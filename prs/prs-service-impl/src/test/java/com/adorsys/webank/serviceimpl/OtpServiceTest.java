package com.adorsys.webank.serviceimpl;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class OtpServiceTest {


    OtpServiceImpl otpServiceImpl = new OtpServiceImpl();

    @Test
    void generateFourDigitOtp() {
        String otp = otpServiceImpl.generateOtp();

        assertNotNull(otp, "Otp should not be null");
        assert otp.length() == 4 : "Otp should be four digits";
        assert otp.matches("\\d+") : "Otp should only contain digits";
        assert Integer.parseInt(otp) >= 1000 && Integer.parseInt(otp) <= 9999 : "Otp should be between 1000 and 9999";
    }

}
