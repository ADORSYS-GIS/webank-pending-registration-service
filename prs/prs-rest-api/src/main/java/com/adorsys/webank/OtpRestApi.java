package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.response.OtpResponse;
import com.adorsys.webank.dto.response.OtpValidationResponse;
import org.springframework.http.ResponseEntity;
import com.adorsys.webank.dto.OtpValidationRequest;
import com.adorsys.webank.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;


@Tag(name = "OTP Management", description = "APIs for OTP generation, sending, and validation")
@RequestMapping("/api/prs/otp")
public interface OtpRestApi {

    @Operation(
        summary = "Generate and send OTP",
        description = "Generates a one-time password and sends it to the provided phone number",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OTP generated and sent successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OtpResponse.class),
                examples = @ExampleObject(
                    name = "successful-otp",
                    value = "{\"otpHash\":\"a1b2c3d4e5f6\",\"phoneNumber\":\"+237691234567\",\"expiresAt\":\"2025-01-20T15:35:00\",\"validitySeconds\":300,\"sent\":true}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "invalid-phone",
                    value = "{\"code\":\"INVALID_PHONE\",\"message\":\"Phone number format is invalid\",\"details\":\"Phone number must start with + followed by country code\",\"timestamp\":\"2025-01-20T15:30:00\",\"path\":\"/api/prs/otp/send\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "unauthorized",
                    value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid or unauthorized JWT\",\"timestamp\":\"2025-01-20T15:30:00\"}"
                )
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
    ResponseEntity<OtpResponse> sendOtp(@RequestBody OtpRequest request);

    @Operation(
        summary = "Validate OTP",
        description = "Validates the provided OTP code against the stored value for the given phone number",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OTP validation result",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OtpValidationResponse.class),
                examples = {
                    @ExampleObject(
                        name = "valid-otp",
                        value = "{\"valid\":true,\"message\":\"OTP validated successfully\",\"details\":null}"
                    ),
                    @ExampleObject(
                        name = "invalid-otp",
                        value = "{\"valid\":false,\"message\":\"Invalid OTP\",\"details\":\"OTP does not match or has expired\"}"
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "missing-otp",
                    value = "{\"code\":\"MISSING_OTP\",\"message\":\"OTP code is required\",\"timestamp\":\"2025-01-20T15:30:00\",\"path\":\"/api/prs/otp/validate\"}"
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
            responseCode = "404",
            description = "OTP not found or expired",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "otp-expired",
                    value = "{\"code\":\"OTP_EXPIRED\",\"message\":\"OTP has expired or does not exist\",\"timestamp\":\"2025-01-20T15:30:00\"}"
                )
            )
        )
    })
    @PostMapping(value = "/validate", consumes = "application/json", produces = "application/json")
    ResponseEntity<OtpValidationResponse> validateOtp( @RequestBody OtpValidationRequest request);
}