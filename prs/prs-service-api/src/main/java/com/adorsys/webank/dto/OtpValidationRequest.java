package com.adorsys.webank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to validate OTP")
public class OtpValidationRequest {
    
    @Schema(description = "Phone number that received the OTP", 
            example = "+237691234567", 
            required = true,
            pattern = "^\\+[1-9]\\d{1,14}$")
    private String phoneNumber;
    
    @Schema(description = "OTP code to validate", 
            example = "123456", 
            required = true,
            pattern = "^\\d{6}$")
    private String otpInput;

}
