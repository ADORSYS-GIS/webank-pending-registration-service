package com.adorsys.webank.service;

import com.adorsys.webank.dto.PendingOtpDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface PendingOtpServiceApi {
    /**
     * Retrieves a list of pending OTP entries from the PRS table.
     *
     * @return List of PendingOtpDto objects containing phone number, masked OTP, and status.
     */
    List<PendingOtpDto> fetchPendingOtpEntries();
}
