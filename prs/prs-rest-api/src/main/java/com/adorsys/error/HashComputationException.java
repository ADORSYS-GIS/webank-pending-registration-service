package com.adorsys.error;

public class HashComputationException extends BaseException {
    public HashComputationException(String message) {
        super(ErrorCode.HASH_COMPUTATION_ERROR, message);
    }
} 