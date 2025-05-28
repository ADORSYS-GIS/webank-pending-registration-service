package com.adorsys.webank.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to validate device registration")
public class DeviceValidateRequest {

    @Schema(description = "Nonce generated during registration initialization", 
            example = "abcdef123456", 
            required = true)
    private String initiationNonce;
    
    @Schema(description = "Proof of work hash", 
            example = "a1b2c3d4e5f6g7h8i9j0", 
            required = true)
    private String powHash;
    
    @Schema(description = "Proof of work nonce", 
            example = "9876543210", 
            required = true)
    private String powNonce;
}
