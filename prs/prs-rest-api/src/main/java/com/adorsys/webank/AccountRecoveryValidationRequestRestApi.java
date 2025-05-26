package com.adorsys.webank;

import com.adorsys.webank.dto.AccountRecovery;
import com.adorsys.webank.dto.AccountRecoveryResponse;
import com.adorsys.webank.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Account Recovery", description = "Endpoints for finalizing account recovery requests")
@RequestMapping("/api/prs/recovery")
@SecurityRequirement(name = "bearer-jwt")
public interface AccountRecoveryValidationRequestRestApi {

    @Operation(
        summary = "Validate recovery token", 
        description = "Processes the final step in account recovery by validating the JWT and associating the new account ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Account recovery validated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountRecoveryResponse.class),
                examples = @ExampleObject(
                    name = "success-recovery",
                    value = "{\"accountId\":\"acc_123456789\",\"status\":\"COMPLETED\",\"message\":\"Account recovery successfully completed\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid token or request data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "invalid-token",
                    value = "{\"code\":\"INVALID_TOKEN\",\"message\":\"Recovery token is invalid or has expired\",\"timestamp\":\"2025-01-20T15:30:00\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized request",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error during validation process",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping(value = "/validate", consumes = "application/json", produces = "application/json")
    ResponseEntity<AccountRecoveryResponse> validateRecoveryToken(
        @Parameter(description = "JWT Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...")
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
        @Parameter(description = "Account recovery validation request", required = true)
        @RequestBody AccountRecovery accountRecovery
    );
}