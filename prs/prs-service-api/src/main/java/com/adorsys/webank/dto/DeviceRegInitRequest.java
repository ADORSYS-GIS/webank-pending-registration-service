package com.adorsys.webank.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to initialize device registration")
public class DeviceRegInitRequest {
    @Schema(description = "Current timestamp in ISO-8601 format", 
            example = "2025-01-20T15:30:00.000Z", 
            required = true)
    private String timeStamp;
}
