package com.adorsys.webank;

import com.adorsys.webank.dto.*;
import com.adorsys.webank.dto.response.KycResponse;
import com.adorsys.webank.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "KYC Management", description = "APIs for Know Your Customer (KYC) verification processes")
@RequestMapping("/api/prs/kyc")
public interface KycRestApi {


    @Operation(
        summary = "Submit KYC Documents",
        description = "Submits identity documents (ID front/back, selfie, tax documents) for KYC verification",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "KYC documents successfully submitted",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = KycResponse.class),
                examples = @ExampleObject(
                    name = "success",
                    value = "{\"kycId\":\"kyc_doc_123456\",\"status\":\"PENDING\",\"submittedAt\":\"2025-01-20T15:30:00\",\"message\":\"KYC documents submitted successfully\",\"verificationDetails\":null}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid document data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "invalid-document",
                    value = "{\"code\":\"INVALID_DOCUMENT\",\"message\":\"Missing required document: frontId\",\"timestamp\":\"2025-01-20T15:30:00\"}"
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
        )
    })
    @PostMapping(value = "/documents", consumes = "application/json", produces = "application/json")
    ResponseEntity<KycResponse> sendKycDocument(
        @Parameter(description = "JWT Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...")
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, 
        @RequestBody KycDocumentRequest kycDocumentRequest
    );


    @Operation(
        summary = "Submit KYC Personal Information",
        description = "Submits personal identification information (ID number, expiry date) for KYC verification",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "KYC information successfully submitted",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = KycResponse.class),
                examples = @ExampleObject(
                    name = "success",
                    value = "{\"kycId\":\"kyc_info_123456\",\"status\":\"PENDING\",\"submittedAt\":\"2025-01-20T15:30:00\",\"message\":\"KYC information submitted successfully\",\"verificationDetails\":null}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid information data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "invalid-id",
                    value = "{\"code\":\"INVALID_ID_NUMBER\",\"message\":\"ID number format is invalid\",\"timestamp\":\"2025-01-20T15:30:00\"}"
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
        )
    })
    @PostMapping(value = "/info", consumes = "application/json", produces = "application/json")
    ResponseEntity<KycResponse> sendKycinfo(
        @Parameter(description = "JWT Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...")
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, 
        @RequestBody KycInfoRequest kycInfoRequest
    );


    @Operation(
        summary = "Submit KYC Location",
        description = "Submits user's location/address information for KYC verification",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "KYC location successfully submitted",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = KycResponse.class),
                examples = @ExampleObject(
                    name = "success",
                    value = "{\"kycId\":\"kyc_loc_123456\",\"status\":\"PENDING\",\"submittedAt\":\"2025-01-20T15:30:00\",\"message\":\"KYC location submitted successfully\",\"verificationDetails\":null}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid location data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "invalid-location",
                    value = "{\"code\":\"INVALID_LOCATION\",\"message\":\"Location information is too short\",\"timestamp\":\"2025-01-20T15:30:00\"}"
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
        )
    })
    @PostMapping(value = "/location", consumes = "application/json", produces = "application/json")
    ResponseEntity<KycResponse> sendKyclocation(
        @Parameter(description = "JWT Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...")
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, 
        @RequestBody KycLocationRequest kycLocationRequest
    );


    @Operation(
        summary = "Submit KYC Email",
        description = "Submits and verifies the email address for KYC purposes",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "KYC email successfully processed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = KycResponse.class),
                examples = @ExampleObject(
                    name = "success",
                    value = "{\"kycId\":\"kyc_email_123456\",\"status\":\"PENDING\",\"submittedAt\":\"2025-01-20T15:30:00\",\"message\":\"Email verification sent to user@example.com\",\"verificationDetails\":null}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid email data",
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
        )
    })
    @PostMapping(value = "/email", consumes = "application/json", produces = "application/json")
    ResponseEntity<KycResponse> sendKycEmail(
        @Parameter(description = "JWT Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...")
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, 
        @RequestBody KycEmailRequest kycEmailRequest
    );

    @Operation(
        summary = "Get Pending KYC Records",
        description = "Fetches all KYC records with PENDING status for verification by authorized agents",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Pending KYC records retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserInfoResponse.class, type = "array"),
                examples = @ExampleObject(
                    name = "pending-records",
                    value = "[{\"accountId\":\"ACC_123\",\"phoneNumber\":\"+237691234567\",\"email\":\"user@example.com\",\"idNumber\":\"ID123456\",\"status\":\"PENDING\",\"createdAt\":\"2025-01-20T15:30:00\"}]"
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
    @GetMapping(value = "/pending", produces = "application/json")
    ResponseEntity<List<UserInfoResponse>> getPendingKycRecords(
        @Parameter(description = "JWT Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...")
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    );

    @Operation(
        summary = "Find KYC Records by Document ID",
        description = "Retrieves KYC records associated with a specific document unique identifier",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Records found successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserInfoResponse.class, type = "array"),
                examples = @ExampleObject(
                    name = "found-records",
                    value = "[{\"accountId\":\"ACC_123\",\"phoneNumber\":\"+237691234567\",\"email\":\"user@example.com\",\"idNumber\":\"ID123456\",\"status\":\"PENDING\",\"createdAt\":\"2025-01-20T15:30:00\"}]"
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
            description = "No records found with the specified document ID",
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
    @GetMapping(value = "/findById/{DocumentUniqueId}", produces = "application/json")
    ResponseEntity<List<UserInfoResponse>> findByDocumentUniqueId(
        @Parameter(description = "JWT Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...")
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
        @Parameter(description = "Unique document identifier", required = true, example = "DOC12345678")
        @PathVariable("DocumentUniqueId") String DocumentUniqueId
    );

}


