package com.adorsys.webank;

import com.adorsys.webank.dto.KycDocumentRequest;
import com.adorsys.webank.dto.KycEmailRequest;
import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.dto.KycLocationRequest;
import com.adorsys.webank.dto.UserInfoResponse;
import com.adorsys.webank.dto.response.ErrorResponse;
import com.adorsys.webank.dto.response.KycDocumentResponse;
import com.adorsys.webank.dto.response.KycEmailResponse;
import com.adorsys.webank.dto.response.KycInfoResponse;
import com.adorsys.webank.dto.response.KycLocationResponse;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

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
                schema = @Schema(implementation = KycDocumentResponse.class),
                examples = @ExampleObject(
                    name = "success",
                    value = "{\"kycId\":\"kyc_doc_123456\",\"status\":\"PENDING\",\"submittedAt\":\"2025-01-20T15:30:00\",\"message\":\"KYC documents submitted successfully\",\"documentStatuses\":[{\"documentType\":\"FRONT_ID\",\"status\":\"PENDING\",\"notes\":null}],\"accountId\":\"ACC_123456\"}"
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
    ResponseEntity<KycDocumentResponse> sendKycDocument(
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
                schema = @Schema(implementation = KycInfoResponse.class),
                examples = @ExampleObject(
                    name = "success",
                    value = "{\"kycId\":\"kyc_info_123456\",\"status\":\"PENDING\",\"submittedAt\":\"2025-01-20T15:30:00\",\"message\":\"KYC information submitted successfully\",\"accountId\":\"ACC_123456\",\"idNumber\":\"ID123456789\",\"expiryDate\":\"2025-12-31\",\"verificationStatus\":\"PENDING\",\"rejectionReason\":null}"
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
    ResponseEntity<KycInfoResponse> sendKycinfo(
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
                schema = @Schema(implementation = KycLocationResponse.class),
                examples = @ExampleObject(
                    name = "success",
                    value = "{\"kycId\":\"kyc_loc_123456\",\"status\":\"PENDING\",\"submittedAt\":\"2025-01-20T15:30:00\",\"message\":\"KYC location submitted successfully\",\"accountId\":\"ACC_123456\",\"location\":\"123 Main St, Apartment 4B, New York, NY 10001\",\"verificationStatus\":\"PENDING\",\"notes\":null}"
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
    ResponseEntity<KycLocationResponse> sendKyclocation(
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
                schema = @Schema(implementation = KycEmailResponse.class),
                examples = @ExampleObject(
                    name = "success",
                    value = "{\"kycId\":\"kyc_email_123456\",\"status\":\"PENDING\",\"submittedAt\":\"2025-01-20T15:30:00\",\"message\":\"Email verification sent to user@example.com\",\"accountId\":\"ACC_123456\",\"email\":\"user@example.com\",\"verificationStatus\":\"VERIFICATION_SENT\",\"verifiedAt\":null}"
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
    ResponseEntity<KycEmailResponse> sendKycEmail(
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
    List<UserInfoResponse> getPendingKycRecords(
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
    List<UserInfoResponse> findByDocumentUniqueId(
        @Parameter(description = "Unique document identifier", required = true, example = "DOC12345678")
        @PathVariable("DocumentUniqueId") String DocumentUniqueId
    );

}


