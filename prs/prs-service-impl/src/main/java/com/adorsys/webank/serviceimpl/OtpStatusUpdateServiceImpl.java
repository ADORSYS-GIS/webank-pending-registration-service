package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.service.OtpStatusUpdateServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OtpStatusUpdateServiceImpl implements OtpStatusUpdateServiceApi {

    private static final Logger log = LoggerFactory.getLogger(OtpStatusUpdateServiceImpl.class);

    @Override
    public String updateOtpStatus(String phoneNumber, String newStatus) {
        // TODO: Update the PRS table record for the given phone number with the new status.
        log.info("Updating OTP status for phone number {} to {}", phoneNumber, newStatus);

        // For now, return a stubbed response.
        return "OTP status for " + phoneNumber + " updated to " + newStatus;
    }
}
