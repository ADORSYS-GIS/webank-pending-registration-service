/*
 * Copyright (c) 2018-2023 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package com.adorsys.webank.security;

import com.adorsys.webank.exceptions.HashComputationException;
import com.nimbusds.jose.jwk.ECKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Centralized utility class for all non-Argon2 hashing operations.
 * Provides methods for SHA-256 hashing with different output encodings.
 */
@Slf4j
@Component
public class HashHelper {
    
    private static final String SHA_256 = "SHA-256";
    
    /**
     * Calculates the SHA-256 hash of the input string and returns it as a hex string.
     * This matches the frontend's CryptoJS.SHA256().toString(CryptoJS.enc.Hex) approach.
     * 
     * @param input The string to hash
     * @return The SHA-256 hash of the input as a hex string
     * @throws HashComputationException If the SHA-256 algorithm is not available
     */
    public String calculateSHA256AsHex(String input) {
        try {
            log.debug("Calculating SHA-256 hash as hex for input");
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error calculating SHA-256 hash", e);
            throw new HashComputationException("Failed to calculate SHA-256 hash: " + e.getMessage());
        }
    }
    
    /**
     * Calculates the SHA-256 hash of the input string and returns it as a Base64URL-encoded string.
     * Useful for JWT Key IDs and other URL-safe applications.
     * 
     * @param input The string to hash
     * @return The SHA-256 hash of the input as a Base64URL-encoded string
     * @throws HashComputationException If the SHA-256 algorithm is not available
     */
    public String calculateSHA256AsBase64Url(String input) {
        try {
            log.debug("Calculating SHA-256 hash as Base64URL for input");
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error calculating SHA-256 hash", e);
            throw new HashComputationException("Failed to calculate SHA-256 hash: " + e.getMessage());
        }
    }
    
    /**
     * Calculates the SHA-256 hash of an ECKey's public JWK representation and returns it as a Base64URL-encoded string.
     * This is specifically for generating Key IDs (kid) for JWTs.
     * 
     * @param serverPublicKey The ECKey to generate a Key ID for
     * @return The SHA-256 hash of the ECKey's public JWK as a Base64URL-encoded string
     * @throws HashComputationException If the SHA-256 algorithm is not available
     */
    public String computeKeyId(ECKey serverPublicKey) {
        if (serverPublicKey == null) {
            throw new IllegalArgumentException("Server public key cannot be null");
        }
        
        try {
            log.debug("Computing Key ID for server public key");
            String publicKeyJson = serverPublicKey.toPublicJWK().toJSONString();
            return calculateSHA256AsBase64Url(publicKeyJson);
        } catch (Exception e) {
            log.error("Error computing Key ID", e);
            throw new HashComputationException("Failed to compute Key ID: " + e.getMessage());
        }
    }
}
