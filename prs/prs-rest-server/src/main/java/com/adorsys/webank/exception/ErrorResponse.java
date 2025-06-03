/*
 * Copyright (c) 2018-2023 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package com.adorsys.webank.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard error response object for API error responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * Error code that can be used by clients to identify the error type
     */
    private String code;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * HTTP status code
     */
    private int status;
}
