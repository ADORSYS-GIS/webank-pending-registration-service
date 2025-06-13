package com.adorsys.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    private int status;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    public static ErrorResponse createErrorResponse(ErrorCode errorCode, String message) {
        return ErrorResponse.builder()
                .code(errorCode.name())
                .message(message != null ? message : errorCode.getMessage())
                .status(errorCode.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse createErrorResponse(ErrorCode errorCode) {
        return createErrorResponse(errorCode, null);
    }
} 