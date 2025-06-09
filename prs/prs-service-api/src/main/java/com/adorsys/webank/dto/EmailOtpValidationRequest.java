package com.adorsys.webank.dto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class EmailOtpValidationRequest {
    private String email;
    private String otpInput;
    private String accountId;
}