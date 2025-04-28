package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpValidationRequest {
    private String phoneNumber;
    private String otpInput;

    public OtpValidationRequest() {}

    public OtpValidationRequest(String phoneNumber, String otpInput) {
        this.phoneNumber = phoneNumber;
        this.otpInput = otpInput;
    }

}
