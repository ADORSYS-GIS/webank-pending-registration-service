package com.adorsys.webank.config;

import org.json.JSONObject;

public class JwtExtractor {
    public static String extractPayloadHash(String payload) {
        // Parse the payload string into a JSONObject
        JSONObject jsonPayload = new JSONObject(payload);

        // Extract the "hash" field from the payload

        // Print the extracted hash
        return jsonPayload.getString("hash");
    }
}
