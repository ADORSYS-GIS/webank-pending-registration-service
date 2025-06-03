package com.adorsys.webank.exceptions;

/**
 * Exception thrown when there's an error generating a KYC certificate.
 */
public class CertificateGenerationException extends RuntimeException {

    public CertificateGenerationException(String message) {
        super(message);
    }

    public CertificateGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
