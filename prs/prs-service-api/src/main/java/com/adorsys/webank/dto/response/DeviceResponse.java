package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Response object for device registration operations")
public class DeviceResponse {
    
    @Schema(description = "Unique device identifier", example = "dev_123456789")
    private String deviceId;
    
    @Schema(description = "Status of the device registration", example = "INITIALIZED")
    private DeviceStatus status;
    
    @Schema(description = "Timestamp when the operation was performed", example = "2025-01-20T15:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "Response message with details about the operation", example = "Device registration initialized successfully")
    private String message;

    public enum DeviceStatus {
        INITIALIZED,
        VALIDATED,
        REJECTED,
        FAILED
    }
    
    // Default constructor
    public DeviceResponse() {
    }
    
    // Constructor with parameters
    public DeviceResponse(String deviceId, DeviceStatus status, LocalDateTime timestamp, String message) {
        this.deviceId = deviceId;
        this.status = status;
        this.timestamp = timestamp;
        this.message = message;
    }
    
    // Getters and setters
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public DeviceStatus getStatus() {
        return status;
    }
    
    public void setStatus(DeviceStatus status) {
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