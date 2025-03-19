package com.adorsys.webank.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter


public class EmailOtpValidationRequest {
    private String email;
    private String otp;
}