package com.adorsys.webank;

import com.adorsys.webank.dto.KycStatusUpdateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

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
    @PostMapping(value = "/status/update", consumes = "application/json", produces = "application/json")
    String updateKycStatus(@RequestBody KycStatusUpdateDto kycStatusUpdateDto);
}
