package com.adorsys.webank.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter


public class EmailOtpValidationRequest {
    private String email;
    private String otp;
    private String accountId;

    public EmailOtpValidationRequest(String email, String otp, String accountId) {
        this.email = email;
        this.otp = otp;
        this.accountId = accountId;
    }
}