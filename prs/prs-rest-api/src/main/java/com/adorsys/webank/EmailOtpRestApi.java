package com.adorsys.webank;


import com.adorsys.webank.dto.EmailOtpRequest;
import com.adorsys.webank.dto.EmailOtpValidationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Email OTP", description = "Operations related to email OTP processing")
@RequestMapping("/api/prs/email-otp")
public interface EmailOtpRestApi {

    @Operation(summary = "Send Email OTP",
            description = "Sends an OTP to the user's email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP successfully sent"),
            @ApiResponse(responseCode = "400", description = "Invalid email address")
    })
    @PostMapping(value = "/send", consumes = "application/json", produces = "application/json")
    String sendEmailOtp(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody EmailOtpRequest request
    );

    @Operation(summary = "Validate Email OTP",
            description = "Validates the received email OTP against the stored value")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP successfully validated"),
            @ApiResponse(responseCode = "400", description = "Invalid OTP")
    })
    @PostMapping(value = "/validate", consumes = "application/json", produces = "application/json")
    String validateEmailOtp(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody EmailOtpValidationRequest request
    );
}