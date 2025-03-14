package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.service.PendingOtpServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class PendingOtpServiceImpl implements PendingOtpServiceApi {

    private static final Logger log = LoggerFactory.getLogger(PendingOtpServiceImpl.class);

    @Override
    public List<PendingOtpDto> getPendingOtps() {
        // TODO: Replace with actual database calls once the PRS table and repository are available.
        log.info("Retrieving pending OTP entries from stubbed data.");

        // Sample stub data: In production, query the database for records where registration is not complete.
        PendingOtpDto pending1 = new PendingOtpDto("+1234567890", maskOtp("123456"), "Pending");
        PendingOtpDto pending2 = new PendingOtpDto("+1987654321", maskOtp("654321"), "Sent");
        return Arrays.asList(pending1, pending2);
    }

    private String maskOtp(String otp) {
        // Example masking: show only the last 2 digits
        if (otp == null || otp.length() < 2) {
            return otp;
        }
        int len = otp.length();
        return "*".repeat(len - 2) +
                otp.substring(len - 2);
    }
}
