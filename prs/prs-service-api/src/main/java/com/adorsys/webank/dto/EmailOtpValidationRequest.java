package com.adorsys.webank.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to validate email OTP")
public class EmailOtpValidationRequest {
    @Schema(description = "Email address that received the OTP", 
            example = "user@example.com", 
            required = true,
            pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    private String email;
    
    @Schema(description = "OTP code to validate", 
            example = "123456", 
            required = true,
            pattern = "^\\d{6}$")
    private String otp;
    
    @Schema(description = "Account identifier for associating the OTP", 
            example = "acc_123456789", 
            required = true)
    private String accountId;

    public EmailOtpValidationRequest(String email, String otp, String accountId) {
        this.email = email;
        this.otp = otp;
        this.accountId = accountId;
    }
}