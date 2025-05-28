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
@Schema(description = "KYC location submission request")
public class KycLocationRequest {
    
    @Schema(description = "User's location/address information", 
            example = "123 Main Street, Douala, Cameroon", 
            required = true,
            minLength = 10,
            maxLength = 500)
    private String location;
    
    @Schema(description = "User's account identifier", 
            example = "ACC_1234567890", 
            required = true)
    private String accountId;
}