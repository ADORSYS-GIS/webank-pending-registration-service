package com.adorsys.webank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object for KYC recovery operations")
public class TokenResponse {

    @Schema(description = "Status", example = "status")
    private String status;
    @Schema(description = "Message", example = "message")
    private String message;
    @Schema(description = "Token", example = "token")
    private String token;

}