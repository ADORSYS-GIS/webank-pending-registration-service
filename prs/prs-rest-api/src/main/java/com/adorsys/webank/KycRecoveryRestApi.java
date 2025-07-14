package com.adorsys.webank;

import com.adorsys.webank.dto.KycRecoveryDto;
import com.adorsys.webank.dto.response.KycRecoveryResponse;
import com.adorsys.webank.dto.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "KYC Recovery Verification", description = "Endpoints for verifying user-provided documents during the KYC recovery process")
@RequestMapping("/api/prs/kyc/recovery")

public interface KycRecoveryRestApi {

    @Operation(summary = "Verify User Documents", description = "Verifies the document ID and expiration date provided by the user during the KYC recovery process.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document verification successful",
                    content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = KycRecoveryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid document details or account ID",
                    content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access",
                    content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/verify", consumes = "application/json", produces = "application/json")
    ResponseEntity<KycRecoveryResponse> verifyKycRecoveryFields(@RequestBody KycRecoveryDto kycRecoveryDto);
}