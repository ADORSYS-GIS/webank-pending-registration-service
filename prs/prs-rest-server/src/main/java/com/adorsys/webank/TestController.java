package com.adorsys.webank;

import com.adorsys.webank.dto.response.OtpResponse;
import com.adorsys.webank.dto.response.ValidationResponse;
import com.adorsys.webank.dto.response.ErrorResponse;
import com.adorsys.webank.dto.response.KycResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test Endpoints", description = "Test endpoints without authentication for Swagger UI demonstration")
public class TestController {

    @Operation(
        summary = "Test OTP Send (No Auth)",
        description = "Test endpoint to demonstrate OTP send response without authentication"
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
                    value = "{\"otpHash\":\"test123hash\",\"phoneNumber\":\"+237691234567\",\"expiresAt\":\"2025-01-20T15:35:00\",\"validitySeconds\":300,\"sent\":true}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid phone number",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/otp/send")
    public ResponseEntity<OtpResponse> testSendOtp(@RequestBody TestOtpRequest request) {
        // Validate phone number format
        if (request.getPhoneNumber() == null || !request.getPhoneNumber().matches("^\\+[1-9]\\d{1,14}$")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        
        // Create mock response
        OtpResponse response = new OtpResponse();
        response.setOtpHash("test" + System.currentTimeMillis() + "hash");
        response.setPhoneNumber(request.getPhoneNumber());
        response.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        response.setValiditySeconds(300);
        response.setSent(true);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Test OTP Validate (No Auth)",
        description = "Test endpoint to demonstrate OTP validation response without authentication"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OTP validation result",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ValidationResponse.class)
            )
        )
    })
    @PostMapping("/otp/validate")
    public ResponseEntity<ValidationResponse> testValidateOtp(@RequestBody TestOtpValidationRequest request) {
        // Mock validation logic
        boolean isValid = "123456".equals(request.getOtpInput());
        
        ValidationResponse response = new ValidationResponse(
            isValid,
            isValid ? "OTP validated successfully" : "Invalid OTP",
            null
        );
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Test Email OTP Send (No Auth)",
        description = "Test endpoint to demonstrate email OTP send without authentication"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Email OTP sent successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "string"),
                examples = @ExampleObject(
                    name = "successful-email-otp",
                    value = "Email OTP sent successfully to user@example.com"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid email address",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/email-otp/send")
    public ResponseEntity<String> testSendEmailOtp(@RequestBody TestEmailOtpRequest request) {
        // Validate email format
        if (request.getEmail() == null || !request.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        return ResponseEntity.ok("Email OTP sent successfully to " + request.getEmail());
    }

    @Operation(
        summary = "Test KYC Document Submission (No Auth)",
        description = "Test endpoint to demonstrate KYC document submission without authentication"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "KYC documents submitted successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = KycResponse.class),
                examples = @ExampleObject(
                    name = "successful-kyc",
                    value = "{\"kycId\":\"kyc_test_123456\",\"status\":\"PENDING\",\"submittedAt\":\"2025-01-20T15:30:00\",\"message\":\"KYC documents submitted successfully\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid document data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/kyc/documents")
    public ResponseEntity<KycResponse> testKycDocumentSubmission(@RequestBody TestKycDocumentRequest request) {
        // Create mock response
        KycResponse response = new KycResponse();
        response.setKycId("kyc_test_" + UUID.randomUUID().toString().substring(0, 8));
        response.setStatus(KycResponse.KycStatus.PENDING);
        response.setSubmittedAt(LocalDateTime.now());
        response.setMessage("KYC documents submitted successfully");
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Test Account Recovery (No Auth)",
        description = "Test endpoint to demonstrate account recovery process without authentication"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Account recovery successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TestAccountRecoveryResponse.class),
                examples = @ExampleObject(
                    name = "successful-recovery",
                    value = "{\"accountId\":\"acc_original_123456\",\"status\":\"COMPLETED\",\"message\":\"Account recovery completed successfully\"}"
                )
            )
        )
    })
    @PostMapping("/recovery/validate")
    public ResponseEntity<TestAccountRecoveryResponse> testAccountRecovery(@RequestBody TestAccountRecoveryRequest request) {
        TestAccountRecoveryResponse response = new TestAccountRecoveryResponse();
        response.setAccountId("acc_original_" + UUID.randomUUID().toString().substring(0, 8));
        response.setStatus("COMPLETED");
        response.setMessage("Account recovery completed successfully");
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Generate Test JWT",
        description = "Generate a test JWT token for testing authenticated endpoints"
    )
    @GetMapping("/generate-token")
    public ResponseEntity<TestTokenResponse> generateTestToken() {
        // This is just a mock token for testing
        String mockToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlRlc3QgVXNlciIsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        
        TestTokenResponse response = new TestTokenResponse();
        response.setToken(mockToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(3600);
        response.setInstruction("Use this token in the Authorization header as: Bearer " + mockToken);
        
        return ResponseEntity.ok(response);
    }

    // Inner classes for request/response DTOs
    @Schema(description = "Test OTP request")
    static class TestOtpRequest {
        @Schema(description = "Phone number", example = "+237691234567", required = true)
        private String phoneNumber;
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    @Schema(description = "Test OTP validation request")
    static class TestOtpValidationRequest {
        @Schema(description = "Phone number", example = "+237691234567", required = true)
        private String phoneNumber;
        @Schema(description = "OTP code", example = "123456", required = true)
        private String otpInput;
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getOtpInput() { return otpInput; }
        public void setOtpInput(String otpInput) { this.otpInput = otpInput; }
    }

    @Schema(description = "Test email OTP request")
    static class TestEmailOtpRequest {
        @Schema(description = "Email address", example = "user@example.com", required = true)
        private String email;
        @Schema(description = "Account ID", example = "acc_123456789", required = true)
        private String accountId;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
    }

    @Schema(description = "Test KYC document request")
    static class TestKycDocumentRequest {
        @Schema(description = "Document type", example = "NATIONAL_ID", required = true)
        private String documentType;
        @Schema(description = "Document data (Base64)", example = "ZG9jdW1lbnQgZGF0YQ==", required = true)
        private String documentData;
        
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public String getDocumentData() { return documentData; }
        public void setDocumentData(String documentData) { this.documentData = documentData; }
    }
    
    @Schema(description = "Test account recovery request")
    static class TestAccountRecoveryRequest {
        @Schema(description = "New account ID", example = "acc_new_123456789", required = true)
        private String newAccountId;
        
        public String getNewAccountId() { return newAccountId; }
        public void setNewAccountId(String newAccountId) { this.newAccountId = newAccountId; }
    }
    
    @Schema(description = "Test account recovery response")
    static class TestAccountRecoveryResponse {
        @Schema(description = "Original account ID", example = "acc_original_123456", required = true)
        private String accountId;
        @Schema(description = "Status of recovery", example = "COMPLETED", required = true)
        private String status;
        @Schema(description = "Recovery message", example = "Account recovery completed successfully")
        private String message;
        
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @Schema(description = "Test token response")
    static class TestTokenResponse {
        @Schema(description = "JWT token", example = "eyJhbGciOiJIUzI1NiIs...")
        private String token;
        @Schema(description = "Token type", example = "Bearer")
        private String tokenType;
        @Schema(description = "Token expiration in seconds", example = "3600")
        private Integer expiresIn;
        @Schema(description = "How to use the token")
        private String instruction;
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        public Integer getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Integer expiresIn) { this.expiresIn = expiresIn; }
        public String getInstruction() { return instruction; }
        public void setInstruction(String instruction) { this.instruction = instruction; }
    }
} 