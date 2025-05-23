package com.adorsys.webank;

import com.adorsys.webank.dto.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "KYC", description = "Operations related to KYC processing")
@RequestMapping("/api/prs/kyc")
public interface KycRestApi {


    @Operation(summary = "Send KYC Document", description = "Sends and processes KYC documents for identity verification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC document successfully processed"),
            @ApiResponse(responseCode = "400", description = "Invalid KYC document data")
    })
    @PostMapping(value = "/documents", consumes = "application/json", produces = "application/json")
    String sendKycDocument(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody KycDocumentRequest kycDocumentRequest);


    @Operation(summary = "Submit KYC Info", description = "Submits personal information required for KYC verification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC information successfully submitted"),
            @ApiResponse(responseCode = "400", description = "Invalid KYC information data")
    })
    @PostMapping(value = "/info", consumes = "application/json", produces = "application/json")
    String sendKycinfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody KycInfoRequest kycInfoRequest);


    @Operation(summary = "Submit KYC Location", description = "Submits location data for KYC verification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC location successfully submitted"),
            @ApiResponse(responseCode = "400", description = "Invalid KYC location data")
    })
    @PostMapping(value = "/location", consumes = "application/json", produces = "application/json")
    String sendKyclocation(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody KycLocationRequest kycLocationRequest);


    @Operation(summary = "Submit KYC Email", description = "Submits and verifies the email address for KYC purposes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC email successfully processed"),
            @ApiResponse(responseCode = "400", description = "Invalid KYC email data")
    })
    @PostMapping(value = "/email", consumes = "application/json", produces = "application/json")
    String sendKycEmail(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody KycEmailRequest kycEmailRequest);

    @Operation(summary = "Get Pending KYC Records",
            description = "Fetches all KYC records with PENDING status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending records retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal error")
    })
    @GetMapping(value = "/pending", produces = "application/json")
    List<UserInfoResponse> getPendingKycRecords(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader);

    @Operation(summary = "Get pending OTPs", description = "Fetches all pending OTPs where registration is not complete. The response includes the phone number, a masked version of the OTP, and the registration status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending OTPs successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(value = "/findById/{DocumentUniqueId}", produces = "application/json")
    List<UserInfoResponse> findByDocumentUniqueId(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                  @PathVariable("DocumentUniqueId") String DocumentUniqueId);

}


