package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpStatusUpdateRequest {
    private String status; // Allowed values: OTP_SENT, OTP_VALIDATED, OTP_FAILED

    public OtpStatusUpdateRequest() {
    }

    public OtpStatusUpdateRequest(String status) {
        this.status = status;
    }

}
