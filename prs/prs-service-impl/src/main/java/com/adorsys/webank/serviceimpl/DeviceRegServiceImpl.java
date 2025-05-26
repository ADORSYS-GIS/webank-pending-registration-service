
package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.service.DeviceRegServiceApi;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.erdtman.jcs.JsonCanonicalizer;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@Service
public class DeviceRegServiceImpl implements DeviceRegServiceApi {
    private static final Logger logger = LoggerFactory.getLogger(DeviceRegServiceImpl.class);

    @Value("${otp.salt}")
    private String salt;

    @Value("${server.private.key}")
    private String SERVER_PRIVATE_KEY_JSON;

    @Value("${server.public.key}")
    private String SERVER_PUBLIC_KEY_JSON;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-time-ms}")
    private Long expirationTimeMs;
    
    private final PasswordEncoder passwordEncoder;
    
    public DeviceRegServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String initiateDeviceRegistration(JWK publicKey, DeviceRegInitRequest regInitRequest) {
        return generateNonce(salt);
    }

    @Override
    public String validateDeviceRegistration(JWK devicePub, DeviceValidateRequest deviceValidateRequest) {
        if (deviceValidateRequest == null) {
            logger.error("DeviceValidateRequest is null");
            return "Error: Missing validation request data";
        }
        
        String initiationNonce = deviceValidateRequest.getInitiationNonce();
        String powNonce = deviceValidateRequest.getPowNonce();
        String newPowHash = deviceValidateRequest.getPowHash();
        
        if (initiationNonce == null || powNonce == null || newPowHash == null) {
            logger.error("Missing required fields in validation request");
            return "Error: Missing required validation fields";
        }
        
        try {
            String nonce = generateNonce(salt);
            
            // Make a JSON object out of initiationNonce, devicePub, powNonce
            String powJSON = "{\"initiationNonce\":\"" + initiationNonce + "\",\"devicePub\":" + devicePub.toJSONString() + ",\"powNonce\":\"" + powNonce + "\"}";
            JsonCanonicalizer jc = new JsonCanonicalizer(powJSON);
            String hashInput = jc.getEncodedString();
            
            // Instead of directly comparing hashes, verify using Argon2 password encoder
            if (!initiationNonce.equals(nonce)) {
                logger.warn("Nonce validation failed: {} != {}", initiationNonce, nonce);
                return "Error: Registration time elapsed, please try again";
            } else if (!passwordEncoder.matches(hashInput, newPowHash)) {
                logger.warn("PoW validation failed for hash: {}", newPowHash);
                return "Error: Verification of PoW failed";
            }

            // Process device certificate
            jc = new JsonCanonicalizer(devicePub.toJSONString());
            String devicePublicKey = jc.getEncodedString();
            logger.info("Generating certificate for device: {}", devicePub.getKeyID());
            return generateDeviceCertificate(devicePublicKey);
            
        } catch (IOException e) {
            logger.error("Error processing JSON: {}", e.getMessage(), e);
            return "Error: Unable to process JSON data";
        } catch (Exception e) {
            logger.error("Error processing device registration: {}", e.getMessage(), e);
            return "Error: Unable to process device registration";
        }
    }
    String calculateHash(String input) {
        // Use Argon2 password encoder for secure hashing
        return passwordEncoder.encode(input);
    }

    public String generateNonce(String salt) {
        if (salt == null) {
            logger.error("Salt cannot be null when generating nonce");
            throw new HashComputationException("Salt cannot be null");
        }
        
        try {
            LocalDateTime timestamp = LocalDateTime.now();
    
            // Flatten the timestamp to the nearest previous 15-minute interval
            int flattenedMinute = (timestamp.getMinute() / 15) * 15;
    
            LocalDateTime flattenedTimestamp = timestamp.withMinute(flattenedMinute).withSecond(0).withNano(0);
    
            String timestampString = flattenedTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String input = timestampString + salt;
    
            logger.debug("Generating nonce with timestamp: {}", timestampString);
            
            // Use secure Argon2 hashing instead of SHA-256
            String hash = passwordEncoder.encode(input);
            
            // We need to extract just the hash part from the Argon2 encoded string
            // The format is {argon2}hash or similar
            String[] parts = hash.split("\\}");
            if (parts.length > 1) {
                String encoded = Base64.getEncoder().encodeToString(parts[1].getBytes(StandardCharsets.UTF_8));
                logger.debug("Generated nonce using Argon2 (extracted): {}", encoded);
                return encoded;
            }
            
            // Fallback to the full hash if we can't extract
            String encoded = Base64.getEncoder().encodeToString(hash.getBytes(StandardCharsets.UTF_8));
            logger.debug("Generated nonce using Argon2 (full): {}", encoded);
            return encoded;
        } catch (Exception e) {
            logger.error("Error computing nonce hash: {}", e.getMessage(), e);
            throw new HashComputationException("Error computing hash: " + e.getMessage());
        }
    }

    String generateDeviceCertificate(String deviceJwkJson) {
        if (deviceJwkJson == null || deviceJwkJson.isEmpty()) {
            logger.error("Device JWK JSON is null or empty");
            throw new IllegalArgumentException("Device JWK JSON cannot be null or empty");
        }
        
        try {
            // Parse the server's private key from the JWK JSON string
            if (SERVER_PRIVATE_KEY_JSON == null || SERVER_PRIVATE_KEY_JSON.isEmpty()) {
                logger.error("Server private key not configured");
                throw new IllegalStateException("Server private key not configured");
            }
            
            ECKey serverPrivateKey = (ECKey) JWK.parse(SERVER_PRIVATE_KEY_JSON);
            if (serverPrivateKey.getD() == null) {
                logger.error("Private key 'd' parameter is missing");
                throw new IllegalStateException("Private key 'd' (private) parameter is missing.");
            }

            // Signer using server's private key
            JWSSigner signer = new ECDSASigner(serverPrivateKey);

            // Parse the server's public key
            if (SERVER_PUBLIC_KEY_JSON == null || SERVER_PUBLIC_KEY_JSON.isEmpty()) {
                logger.error("Server public key not configured");
                throw new IllegalStateException("Server public key not configured");
            }
            
            ECKey serverPublicKey = (ECKey) JWK.parse(SERVER_PUBLIC_KEY_JSON);

            // Use Argon2 for more secure key ID hashing
            String publicKeyString = serverPublicKey.toPublicJWK().toJSONString();
            String encodedHash = passwordEncoder.encode(publicKeyString);
            // Extract and encode as URL-safe Base64 for KID
            String kid = Base64.getUrlEncoder().withoutPadding().encodeToString(
                encodedHash.getBytes(StandardCharsets.UTF_8));
            logger.debug("Generated key ID: {}", kid);

            // Create JWT Header
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(kid) // Set KID using Argon2 hash
                    .type(JOSEObjectType.JWT)
                    .build();

            // Parse device's public JWK
            JWK deviceJwk;
            try {
                deviceJwk = JWK.parse(deviceJwkJson);
                logger.debug("Parsed device JWK with key ID: {}", deviceJwk.getKeyID());
            } catch (ParseException e) {
                logger.error("Failed to parse device JWK: {}", e.getMessage());
                throw new IllegalStateException("Error parsing device JWK", e);
            }

            // Create JWT Payload
            long issuedAt = System.currentTimeMillis() / 60000; // Convert to seconds
            logger.debug("Certificate issued at: {}", issuedAt);
            
            // Check if expirationTimeMs is configured
            if (expirationTimeMs == null || expirationTimeMs <= 0) {
                logger.warn("Expiration time not properly configured, using default (24h)");
                expirationTimeMs = 86400000L; // 24 hours in milliseconds
            }
            
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer("https://webank.com")  // Fixed issuer format
                    .audience(deviceJwk.getKeyID()) // Use device public key ID as audience
                    .claim("cnf", Collections.singletonMap("jwk", deviceJwk.toJSONObject())) // Fix JSON structure
                    .issueTime(new Date(issuedAt * 1000))
                    .expirationTime(new Date((issuedAt + (expirationTimeMs / 1000)) * 1000)) // Convert to milliseconds
                    .build();

            // Create JWT token
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);

            String cert = signedJWT.serialize();
            logger.info("Generated device certificate successfully");
            logger.debug("Certificate: {}", cert);
            return cert;

        } catch (JOSEException e) {
            logger.error("JWT processing error: {}", e.getMessage(), e);
            throw new IllegalStateException("Error signing device certificate", e);
        } catch (ParseException e) {
            logger.error("Failed to parse JWK: {}", e.getMessage(), e);
            throw new IllegalStateException("Error parsing JWK", e);
        } catch (Exception e) {
            logger.error("Unexpected error generating device certificate: {}", e.getMessage(), e);
            throw new IllegalStateException("Error generating device certificate: " + e.getMessage(), e);
        }
    }

}
