package com.adorsys.webank;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import com.adorsys.webank.dto.KycInfoRequest;

@Tag(name = "OTP Status Update", description = "Operations to update OTP status for pending registrations")
@RequestMapping("/api/prs/kyc")
public interface KycStatusUpdateRestApi {

    @Operation(summary = "Update OTP status", description = "Updates the OTP status for the specified phone number. Accepts a JSON payload with the new status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid phone number or status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping(value = "/{accountId}/{status}", consumes = "application/json", produces = "application/json")
    String updateKycStatus(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                           @PathVariable("accountId") String accountId,
                           @PathVariable("status") String status,
                           @RequestBody KycInfoRequest kycInfoRequest);
}
