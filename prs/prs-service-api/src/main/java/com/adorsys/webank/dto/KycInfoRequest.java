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
@Schema(description = "KYC information submission request")
public class KycInfoRequest {

    @Schema(description = "National ID or passport number", 
            example = "ID123456789", 
            required = true,
            minLength = 5,
            maxLength = 20)
    private String idNumber;
    
    @Schema(description = "Document expiry date in ISO format", 
            example = "2025-12-31", 
            required = true,
            pattern = "^\\d{4}-\\d{2}-\\d{2}$")
    private String expiryDate;
    
    @Schema(description = "User's account identifier", 
            example = "ACC_1234567890", 
            required = true)
    private String accountId;
    
    @Schema(description = "Reason for rejection if applicable", 
            example = "Document expired")
    private String rejectionReason;
    
    @Schema(description = "Additional notes about rejection", 
            example = "Please provide a valid document")
    private String rejectionNote;
}
