package com.adorsys.webank;

import com.adorsys.webank.dto.AccountRecoveryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Account Recovery", description = "Endpoints for finalizing account recovery requests")
@RequestMapping("/api/prs/recovery")
public interface AccountRecoveryValidationRequestRestApi {

    @Operation(summary = "Validate recovery token", description = "Processes the final step in account recovery by validating the JWT and associating the new account ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account recovery validated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token or request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized request"),
            @ApiResponse(responseCode = "500", description = "Internal server error during validation process")
    })
    @PostMapping(value = "/validate", consumes = "application/json", produces = "application/json")
    ResponseEntity<AccountRecoveryResponse> validateRecoveryToken(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody String newAccountId
    );
}