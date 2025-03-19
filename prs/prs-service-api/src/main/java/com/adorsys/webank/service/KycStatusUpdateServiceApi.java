package com.adorsys.webank.service;

import org.springframework.stereotype.Service;

@Service
public interface OtpStatusUpdateServiceApi {
    /**
     * Updates the OTP status for the specified phone number.
     *
     * @param phoneNumber The phone number for which to update the OTP status.
     * @param newStatus   The new status to set (e.g., OTP_SENT, OTP_VALIDATED, OTP_FAILED).
     * @return A confirmation message.
     */
    String updateOtpStatus(String phoneNumber, String newStatus);
}
