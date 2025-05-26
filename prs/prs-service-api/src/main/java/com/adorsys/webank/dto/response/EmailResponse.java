package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Response object for email operations")
public class EmailResponse {
    
    @Schema(description = "Unique identifier for the email operation", example = "email_op_123456")
    private String operationId;
    
    @Schema(description = "Status of the email operation", example = "SUCCESS")
    private EmailStatus status;
    
    @Schema(description = "Timestamp when the operation was performed", example = "2025-01-20T15:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "Response message with details about the operation", example = "Email OTP sent successfully")
    private String message;

    public enum EmailStatus {
        SUCCESS,
        PENDING,
        FAILED
    }
    
    // Default constructor
    public EmailResponse() {
    }
    
    // Constructor with parameters
    public EmailResponse(String operationId, EmailStatus status, LocalDateTime timestamp, String message) {
        this.operationId = operationId;
        this.status = status;
        this.timestamp = timestamp;
        this.message = message;
    }
    
    // Getters and setters
    public String getOperationId() {
        return operationId;
    }
    
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }
    
    public EmailStatus getStatus() {
        return status;
    }
    
    public void setStatus(EmailStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
} 