package com.adorsys.webank.exceptions;

public class JwtPayloadParseException extends RuntimeException {
    public JwtPayloadParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
