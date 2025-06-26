package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.dto.response.DeviceResponse;
import com.adorsys.webank.dto.response.DeviceValidationResponse;
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

@Tag(name = "Device Registration", description = "Operations related to device registration processing")
@RequestMapping("/api/prs/dev")
@SecurityRequirement(name = "bearer-jwt")
public interface DeviceRegRestApi {

    @Operation(
        summary = "Initialize device registration", 
        description = "Starts the device registration process by generating a unique nonce for device validation"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Device registration initialized successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DeviceResponse.class),
                examples = @ExampleObject(
                    name = "successful-init",
                    value = "{\"status\":\"INITIALIZED\",\"timestamp\":\"2025-01-20T15:30:00\",\"message\":\"Device registration initialized. Use the following nonce for validation: abc123def456\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid device information",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping(value = "/init", consumes = "application/json", produces = "application/json")
    ResponseEntity<DeviceResponse> initiateDeviceRegistration(
        @RequestBody DeviceRegInitRequest request
    );

    @Operation(
        summary = "Validate device", 
        description = "Validates a device during the registration process using proof of work and returns a device certificate"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Device validated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DeviceValidationResponse.class),
                examples = @ExampleObject(
                    name = "successful-validation",
                    value = "{\"status\":\"VALIDATED\",\"timestamp\":\"2025-01-20T15:30:00\",\"certificate\":\"eyJhbGciOiJFUzI1NiIsImtpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJKV1QifQ...\",\"message\":\"Device successfully validated and certificate issued\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid device validation data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Device not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping(value = "/validate", consumes = "application/json", produces = "application/json")
    ResponseEntity<DeviceValidationResponse> validateDeviceRegistration(
        @RequestBody DeviceValidateRequest request
    );
}