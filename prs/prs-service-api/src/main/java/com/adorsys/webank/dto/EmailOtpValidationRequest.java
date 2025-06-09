package com.adorsys.webank.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter


public class EmailOtpValidationRequest {
    private String email;
    private String otpInput;
    private String accountId;

    public EmailOtpValidationRequest(String email, String otpInput, String accountId) {
        this.email = email;
        this.otpInput = otpInput;
        this.accountId = accountId;
    }
}