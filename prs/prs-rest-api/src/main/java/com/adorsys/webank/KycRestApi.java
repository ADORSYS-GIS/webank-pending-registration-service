package com.adorsys.webank;

import com.adorsys.webank.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

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
}
