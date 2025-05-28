package com.adorsys.webank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Account recovery response")
public class AccountRecoveryResponse {
    @Schema(description = "Original account ID that was recovered", 
            example = "acc_original_123456", 
            required = true)
    private String accountId;
    
    @Schema(description = "New KYC certificate generated during recovery", 
            example = "kyc_cert_abcdef123456")
    private String kycCertificate;
    
    @Schema(description = "Status of the recovery process", 
            example = "COMPLETED", 
            required = true)
    private String status;
    
    @Schema(description = "Descriptive message about the recovery process", 
            example = "Account recovery successfully completed")
    private String message;

    public AccountRecoveryResponse(String accountId, String kycCertificate, String message) {
        this.accountId = accountId;
        this.kycCertificate = kycCertificate;
        this.status = "COMPLETED";
        this.message = message;
    }
}
