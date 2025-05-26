package com.adorsys.webank.security;

import org.json.JSONObject;

public class JwtExtractor {
    public static String extractPayloadHash(String payload) {
        try {
            // Parse the payload string into a JSONObject
            JSONObject jsonPayload = new JSONObject(payload);

            // Extract the "hash" field from the payload
            if (!jsonPayload.has("hash")) {
                throw new IllegalArgumentException("Payload does not contain 'hash' field");
            }

            // Return the extracted hash
            return jsonPayload.getString("hash");
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to extract hash from payload: " + e.getMessage(), e);
        }
    }
}
