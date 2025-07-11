package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response for KYC status update operation")
public class KycStatusUpdateResponse {

    @Schema(description = "Indicates if the KYC status update was successful", example = "true", required = true)
    private boolean success;

    @Schema(description = "Message describing the result of the KYC status update", example = "KYC status updated successfully to VERIFIED", required = true)
    private String message;

    @Schema(description = "The updated KYC status", example = "VERIFIED")
    private String status;

    @Schema(description = "Account ID associated with the KYC update", example = "ACC123456789")
    private String accountId;
}