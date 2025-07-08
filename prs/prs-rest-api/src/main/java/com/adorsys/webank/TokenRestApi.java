package com.adorsys.webank;

import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.dto.TokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Recovery", description = "Operations related to account recovery and token management")
@RequestMapping("/api/prs/kyc/recovery")
public interface TokenRestApi {

    @Operation(summary = "Request Recovery Token", description = "Generates and returns a recovery token to reset account credentials or regain access.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recovery token successfully generated"),
            @ApiResponse(responseCode = "400", description = "Bad request - Missing or invalid parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired authorization token"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/token", produces = "application/json")
    String requestRecoveryToken(@RequestBody TokenRequest tokenRequest);
}