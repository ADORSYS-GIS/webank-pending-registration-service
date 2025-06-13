package com.adorsys.webank.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class EmailOtpValidationRequest {
    @Schema(description = "Email address that received the OTP", 
            example = "user@example.com", 
            required = true,
            pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    private String email;
    private String otpInput;
    private String accountId;
}