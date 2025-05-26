package com.adorsys.webank.serviceimpl.security;

import com.adorsys.webank.security.JwtExtractor;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtExtractorTest {

    @Test
    void extractPayloadHash_validHash_returnsHash() {
        String payload = "{\"hash\":\"testHash\"}";
        String hash = JwtExtractor.extractPayloadHash(payload);
        assertEquals("testHash", hash);
    }

    @Test
    void extractPayloadHash_missingHash_throwsException() {
        String payload = "{\"noHash\":\"value\"}";
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> JwtExtractor.extractPayloadHash(payload));
        assertTrue(exception.getMessage().contains("does not contain 'hash' field"));
    }

    @Test
    void extractPayloadHash_emptyPayload_throwsException() {
        String payload = "{}";
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> JwtExtractor.extractPayloadHash(payload));
        assertTrue(exception.getMessage().contains("does not contain 'hash' field"));
    }

    @Test
    void extractPayloadHash_nullPayload_throwsException() {
        String payload = null;
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> JwtExtractor.extractPayloadHash(payload));
        assertTrue(exception.getMessage().contains("Failed to extract hash from payload"));
    }

    @Test
    void extractPayloadHash_invalidJson_throwsException() {
        String payload = "invalid json";
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> JwtExtractor.extractPayloadHash(payload));
        assertTrue(exception.getMessage().contains("Failed to extract hash from payload"));
    }
}