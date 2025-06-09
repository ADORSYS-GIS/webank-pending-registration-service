package com.adorsys.webank.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Map;

public final class JwtExtractor {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JwtExtractor.class);

    // Private constructor to prevent instantiation
    private JwtExtractor() {
        throw new IllegalStateException("Utility class");
    }

    public static String extractPayloadHash(String payload) {
        try {
            // Parse the payload string into a Map
            Map<String, Object> jsonPayload = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});

            // Extract the "hash" field from the payload
            Object hashObject = jsonPayload.get("hash");

            if (hashObject instanceof String) {
                return (String) hashObject;
            } else if (hashObject != null) {
                // Handle cases where hash might not be a string, or log a warning
                // For now, returning its string representation or null if unexpected type
                logger.warn("'hash' field in JWT payload is not a String: {}", hashObject.getClass().getName());
                return hashObject.toString();
            } else {
                // Handle missing hash field - throw exception or return null based on requirements
                logger.warn("'hash' field not found in JWT payload.");
                return null; 
            }
        } catch (IOException e) {
            // Handle parsing exception - log and rethrow
            logger.error("Error parsing JWT payload: {}", e.getMessage(), e);
            throw new com.adorsys.webank.exceptions.JwtPayloadParseException("Failed to parse JWT payload", e);
        }
    }
}
