package com.adorsys.webank;

import com.adorsys.webank.dto.EmailOtpRequest;
import com.adorsys.webank.dto.EmailOtpValidationRequest;
import com.adorsys.webank.dto.response.EmailResponse;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Email OTP", description = "Operations related to email OTP processing")
@RequestMapping("/api/prs/email-otp")
@SecurityRequirement(name = "bearer-jwt")
public interface EmailOtpRestApi {

    @Operation(
        summary = "Send Email OTP",
        description = "Sends a one-time password to the user's email address for verification"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "OTP successfully sent",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EmailResponse.class),
                examples = @ExampleObject(
                    name = "successful-otp",
                    value = "{\"operationId\":\"email_op_123456\",\"status\":\"SUCCESS\",\"timestamp\":\"2025-01-20T15:30:00\",\"message\":\"Email OTP sent successfully to user@example.com\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid email address",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "invalid-email",
                    value = "{\"code\":\"INVALID_EMAIL\",\"message\":\"Email format is invalid\",\"timestamp\":\"2025-01-20T15:30:00\"}"
                )
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
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping(value = "/send", consumes = "application/json", produces = "application/json")
    ResponseEntity<EmailResponse> sendEmailOtp(
        @Parameter(description = "JWT Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...")
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, 
        @RequestBody EmailOtpRequest request
    );

    @Operation(
        summary = "Validate Email OTP",
        description = "Validates the received email OTP against the stored value"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "OTP successfully validated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EmailResponse.class),
                examples = @ExampleObject(
                    name = "validation-success",
                    value = "{\"operationId\":\"email_op_987654\",\"status\":\"SUCCESS\",\"timestamp\":\"2025-01-20T15:30:00\",\"message\":\"Email OTP validated successfully\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid OTP",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "invalid-otp",
                    value = "{\"code\":\"INVALID_OTP\",\"message\":\"The OTP is invalid or has expired\",\"timestamp\":\"2025-01-20T15:30:00\"}"
                )
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
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping(value = "/validate", consumes = "application/json", produces = "application/json")
    ResponseEntity<EmailResponse> validateEmailOtp(
        @Parameter(description = "JWT Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...")
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, 
        @RequestBody EmailOtpValidationRequest request
    );
}