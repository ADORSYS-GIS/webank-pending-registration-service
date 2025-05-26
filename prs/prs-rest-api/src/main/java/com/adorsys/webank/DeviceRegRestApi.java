package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.dto.response.DeviceResponse;
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
        description = "Starts the device registration process by generating a unique device identifier"
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
                    value = "{\"deviceId\":\"dev_123456789\",\"status\":\"INITIALIZED\",\"timestamp\":\"2025-01-20T15:30:00\",\"message\":\"Device registration initialized successfully\"}"
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
    ResponseEntity<DeviceResponse> initDeviceRegistration(
        @Parameter(description = "JWT Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...")
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, 
        @RequestBody DeviceRegInitRequest request
    );

    @Operation(
        summary = "Validate device", 
        description = "Validates a device during the registration process"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Device validated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DeviceResponse.class),
                examples = @ExampleObject(
                    name = "successful-validation",
                    value = "{\"deviceId\":\"dev_123456789\",\"status\":\"VALIDATED\",\"timestamp\":\"2025-01-20T15:30:00\",\"message\":\"Device validated successfully\"}"
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
    ResponseEntity<DeviceResponse> validateDevice(
        @Parameter(description = "JWT Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...")
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, 
        @RequestBody DeviceValidateRequest request
    );
}