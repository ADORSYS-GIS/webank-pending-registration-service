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
        assertThrows(JSONException.class, () -> JwtExtractor.extractPayloadHash(payload));
    }
}