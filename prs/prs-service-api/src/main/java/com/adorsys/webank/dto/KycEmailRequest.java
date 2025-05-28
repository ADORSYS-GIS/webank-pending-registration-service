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
@Schema(description = "KYC email verification request")
public class KycEmailRequest {
    
    @Schema(description = "User's email address for verification", 
            example = "user@example.com", 
            required = true,
            pattern = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")
    private String email;
    
    @Schema(description = "User's account identifier", 
            example = "ACC_1234567890", 
            required = true)
    private String accountId;
}
