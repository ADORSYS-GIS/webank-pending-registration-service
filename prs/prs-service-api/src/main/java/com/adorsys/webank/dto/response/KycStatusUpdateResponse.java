package com.adorsys.webank.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serial;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Response DTO for KYC status update operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KycStatusUpdateResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Indicates if the operation was successful
     */
    private boolean success;

    /**
     * Status message (success or error message)
     */
    private String message;

    /**
     * The updated KYC status
     */
    private String status;

    /**
     * Account ID associated with the KYC
     */
    private String accountId;

    /**
     * Document ID number
     */
    private String documentId;

    /**
     * Timestamp of the status update
     */
    private LocalDateTime timestamp;

    /**
     * Creates a success response
     */
    public static KycStatusUpdateResponse success(String message, String status, String accountId, String documentId) {
        return KycStatusUpdateResponse.builder()
                .success(true)
                .message(message)
                .status(status)
                .accountId(accountId)
                .documentId(documentId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response
     */
    public static KycStatusUpdateResponse error(String message, String accountId, String documentId) {
        return KycStatusUpdateResponse.builder()
                .success(false)
                .message(message)
                .accountId(accountId)
                .documentId(documentId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
