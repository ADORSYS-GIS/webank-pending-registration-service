package com.adorsys.webank;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "KYC", description = "Operations related to KYC processing")
@RequestMapping("/api/prs/kyc")
public interface KycCertRestApi {

    @Operation(summary = "Get KYC Certificate", description = "Retrieves the KYC certificate if the user's status is approved.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC certificate successfully retrieved"),
            @ApiResponse(responseCode = "404", description = "KYC certificate not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid token"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(value = "/cert/{AccountId}", produces = "application/json")
    String getCert(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                   @PathVariable("AccountId") String AccountId);
}
