package com.adorsys.webank.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to generate and send email OTP")
public class EmailOtpRequest {
    @Schema(description = "Email address to send OTP to", 
            example = "user@example.com", 
            required = true,
            pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    private String email;
    
    @Schema(description = "Account identifier for associating the OTP", 
            example = "acc_123456789", 
            required = true)
    private String accountId;

    public EmailOtpRequest(String email, String accountId) {
        this.email = email;
        this.accountId = accountId;
    }
}
