package com.adorsys.webank;

import com.adorsys.webank.dto.PendingOtpDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "OTP Retrieval", description = "Operations related to retrieving pending OTPs for tellers")
@RequestMapping("/api/prs/otp")
public interface PendingOtpListRestApi {

    @Operation(summary = "Get pending OTPs", description = "Fetches all pending OTPs where registration is not complete. The response includes the phone number, a masked version of the OTP, and the registration status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending OTPs successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(value = "/pending", produces = "application/json")
    List<PendingOtpDto> getPendingOtps();
}
