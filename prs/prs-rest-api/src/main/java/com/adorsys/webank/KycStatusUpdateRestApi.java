package com.adorsys.webank;

import com.adorsys.webank.dto.KycStatusUpdateDto;
import com.adorsys.webank.dto.response.KycStatusUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * REST API for managing KYC status updates.
 * Provides endpoints for updating the status of KYC verifications.
 */
@Tag(
    name = "KYC Status Update", 
    description = "Operations to update KYC verification status for pending registrations"
)
@RequestMapping(
    value = "/api/prs/kyc",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public interface KycStatusUpdateRestApi {

    /**
     * Updates the KYC verification status for the specified account.
     *
     * @param kycStatusUpdateDto The DTO containing the KYC status update information
     * @return ResponseEntity containing the result of the KYC status update operation
     */
    @Operation(
        summary = "Update KYC verification status",
        description = "Updates the KYC verification status for the specified account. " +
                    "Requires valid document verification details.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "KYC status updated successfully",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = KycStatusUpdateResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters or document verification failed",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = KycStatusUpdateResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "KYC record not found for the specified account",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = KycStatusUpdateResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = KycStatusUpdateResponse.class)
                )
            )
        }
    )
    @PostMapping(
        value = "/status/update",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<KycStatusUpdateResponse> updateKycStatus(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "KYC status update details",
            required = true,
            content = @Content(
                schema = @Schema(implementation = KycStatusUpdateDto.class)
            )
        )
        @RequestBody KycStatusUpdateDto kycStatusUpdateDto
    );
}
