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
@Schema(description = "Request to generate and send OTP")
public class OtpRequest {
    
    @Schema(description = "Phone number to send OTP to", 
            example = "+237691234567", 
            required = true,
            pattern = "^\\+[1-9]\\d{1,14}$")
    private String phoneNumber;
}
