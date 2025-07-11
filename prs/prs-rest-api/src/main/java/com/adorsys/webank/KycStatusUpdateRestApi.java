package com.adorsys.webank;

import com.adorsys.webank.dto.KycStatusUpdateDto;
import com.adorsys.webank.dto.response.ErrorResponse;
import com.adorsys.webank.dto.response.KycStatusUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "KYC Status Update", description = "APIs to update KYC status for pending registrations")
@RequestMapping("/api/prs/kyc")
public interface KycStatusUpdateRestApi {

    @Operation(
            summary = "Update KYC status",
            description = "Updates the KYC status for the specified account. Accepts a JSON payload with the new status and related info.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "KYC status updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = KycStatusUpdateResponse.class),
                            examples = @ExampleObject(
                                    name = "success-kyc-update",
                                    value = "{\"success\":true,\"message\":\"KYC status updated successfully\",\"details\":null,\"newStatus\":\"VERIFIED\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "invalid-status",
                                            value = "{\"code\":\"INVALID_STATUS\",\"message\":\"Invalid KYC status provided\",\"details\":\"Status must be one of: PENDING, VERIFIED, REJECTED\",\"timestamp\":\"2025-01-20T15:30:00\",\"path\":\"/api/prs/kyc/status/update\"}"
                                    ),
                                    @ExampleObject(
                                            name = "missing-rejection-reason",
                                            value = "{\"code\":\"MISSING_REJECTION_REASON\",\"message\":\"Rejection reason is required when status is REJECTED\",\"timestamp\":\"2025-01-20T15:30:00\",\"path\":\"/api/prs/kyc/status/update\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "unauthorized",
                                    value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid or unauthorized JWT\",\"timestamp\":\"2025-01-20T15:30:00\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No KYC record found for the given account",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "kyc-record-not-found",
                                    value = "{\"code\":\"KYC_RECORD_NOT_FOUND\",\"message\":\"No KYC record found for the provided accountId\",\"timestamp\":\"2025-01-20T15:30:00\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping(value = "/status/update", consumes = "application/json", produces = "application/json")
    ResponseEntity<KycStatusUpdateResponse> updateKycStatus(@RequestBody KycStatusUpdateDto kycStatusUpdateDto);
}