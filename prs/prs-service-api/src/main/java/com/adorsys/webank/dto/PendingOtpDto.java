package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PendingOtpDto {
    private String phoneNumber;
    private String otpCode;
    private String status; // e.g., "Pending", "Sent", "Completed"

    public PendingOtpDto() {
    }

    public PendingOtpDto(String phoneNumber, String otpCode, String status) {
        this.phoneNumber = phoneNumber;
        this.otpCode = otpCode;
        this.status = status;
    }
}
