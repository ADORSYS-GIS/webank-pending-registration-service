package com.adorsys.webank;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import com.adorsys.webank.dto.KycInfoRequest;

@Tag(name = "KYC Recovery Verification", description = "Endpoints for verifying user-provided documents during the KYC recovery process")
@RequestMapping("/api/prs/kyc/recovery")

public interface KycRecoveryRestApi {

    @Operation(summary = "Verify User Documents", description = "Verifies the document ID and expiration date provided by the user during the KYC recovery process.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document verification successful"),
            @ApiResponse(responseCode = "400", description = "Invalid document details or account ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/{accountId}", consumes = "application/json", produces = "application/json")
    String verifyKycRecoveryFields(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                           @PathVariable("accountId") String accountId,
                           @RequestBody KycInfoRequest kycInfoRequest);
}