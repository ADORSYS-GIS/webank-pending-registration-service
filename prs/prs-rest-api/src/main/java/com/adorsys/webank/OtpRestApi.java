package com.adorsys.webank;

import com.adorsys.webank.dto.OtpRequest;
import com.adorsys.webank.dto.OtpValidationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "OTP", description = "Operations related to OTP processing")
@RequestMapping("/api/otp")
public interface OtpRestApi {

    @Operation(summary = "Send OTP", description = "Sends an OTP to the user's phone number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP successfully sent"),
            @ApiResponse(responseCode = "400", description = "Invalid phone number")
    })
    @PostMapping(value = "/send", consumes = "application/json", produces = "application/json")
    String sendOtp(@RequestBody OtpRequest request);

    @Operation(summary = "Validate OTP", description = "Validates the received OTP against the stored value")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP successfully validated"),
            @ApiResponse(responseCode = "400", description = "Invalid OTP")
    })
    @PostMapping(value = "/validate", consumes = "application/json", produces = "application/json")
    boolean validateOtp(@RequestBody OtpValidationRequest request);
}
