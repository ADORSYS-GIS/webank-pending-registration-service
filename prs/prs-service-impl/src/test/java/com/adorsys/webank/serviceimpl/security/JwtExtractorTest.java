package com.adorsys.webank.serviceimpl.security;

import com.adorsys.webank.config.JwtExtractor;
import com.adorsys.webank.exceptions.JwtPayloadParseException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtExtractorTest {

    @Test
    void extractPayloadHashValidHashReturnsHash() {
        String payload = "{\"hash\":\"testHash\"}";
        String hash = JwtExtractor.extractPayloadHash(payload);
        assertEquals("testHash", hash);
    }

    @Test
    void extractPayloadHashMissingHashReturnsNull() { // Renamed for clarity
        String payload = "{\"noHash\":\"value\"}";
        String hash = JwtExtractor.extractPayloadHash(payload);
        assertNull(hash);
    }

    @Test
    void extractPayloadHashEmptyPayloadReturnsNull() { // Renamed for clarity
        String payload = "{}";
        String hash = JwtExtractor.extractPayloadHash(payload);
        assertNull(hash);
    }

    @Test
    void extractPayloadHashNullPayloadThrowsIllegalArgumentException() { // Renamed for clarity
        String payload = null;
        assertThrows(IllegalArgumentException.class, () -> JwtExtractor.extractPayloadHash(payload));
    }

    @Test
    void extractPayloadHashInvalidJsonThrowsJwtPayloadParseException() { // Renamed for clarity and new exception
        String payload = "invalid json";
        assertThrows(JwtPayloadParseException.class, () -> JwtExtractor.extractPayloadHash(payload));
    }
}