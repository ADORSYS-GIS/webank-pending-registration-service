package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PendingOtpDto {
    private String phoneNumber;
    private String maskedOtp;
    private String status; // e.g., "Pending", "Sent", "Completed"

    public PendingOtpDto() {
    }

    public PendingOtpDto(String phoneNumber, String maskedOtp, String status) {
        this.phoneNumber = phoneNumber;
        this.maskedOtp = maskedOtp;
        this.status = status;
    }

}
