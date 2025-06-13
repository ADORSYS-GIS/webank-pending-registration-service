package com.adorsys.error;

public class AccountNotFoundException extends BaseException {
    public AccountNotFoundException(String message) {
        super(ErrorCode.ACCOUNT_NOT_FOUND, message);
    }
} 