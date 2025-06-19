package com.adorsys.webank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KYC information submission request")
public class KycInfoRequest {

    @NotBlank
    @Size(min = 5, max = 20)
    @Schema(description = "National ID or passport number", 
            example = "ID123456789", 
            required = true)
    private String idNumber;
    
    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    @Schema(description = "Document expiry date in ISO format", 
            example = "2025-12-31", 
            required = true)
    private String expiryDate;
    
    @NotBlank
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
