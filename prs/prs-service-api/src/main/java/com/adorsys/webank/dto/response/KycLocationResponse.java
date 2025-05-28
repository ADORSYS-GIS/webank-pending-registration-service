package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KYC location submission response")
public class KycLocationResponse {
    
    @Schema(description = "Unique KYC location record identifier", example = "kyc_loc_1234567890", required = true)
    private String kycId;
    
    @Schema(description = "KYC processing status", example = "PENDING", required = true)
    private KycResponse.KycStatus status;
    
    @Schema(description = "Submission timestamp", example = "2025-01-20T15:30:00")
    private LocalDateTime submittedAt;
    
    @Schema(description = "Additional processing message", example = "KYC location submitted successfully")
    private String message;
    
    @Schema(description = "Account ID associated with the location", example = "ACC_1234567890")
    private String accountId;
    
    @Schema(description = "Full location/address details", example = "123 Main St, Apartment 4B, New York, NY 10001")
    private String location;
    
    @Schema(description = "Verification status of the location", example = "VERIFIED")
    private VerificationStatus verificationStatus;
    
    @Schema(description = "Additional verification notes", example = "Address format valid")
    private String notes;
    
    public enum VerificationStatus {
        @Schema(description = "Location pending verification")
        PENDING,
        @Schema(description = "Location verified successfully")
        VERIFIED,
        @Schema(description = "Location verification failed")
        FAILED,
        @Schema(description = "Location details incomplete")
        INCOMPLETE
    }
} 