
package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;

import com.adorsys.webank.service.DeviceRegServiceApi;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.erdtman.jcs.JsonCanonicalizer;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@Service
public class DeviceRegServiceImpl implements DeviceRegServiceApi {
    private static final Logger logger = LoggerFactory.getLogger(DeviceRegServiceImpl.class);

    private final PasswordHashingService passwordHashingService;

    @Value("${server.private.key}")
    private String SERVER_PRIVATE_KEY_JSON;

    @Value("${server.public.key}")
    private String SERVER_PUBLIC_KEY_JSON;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-time-ms}")
    private Long expirationTimeMs;
    
    public DeviceRegServiceImpl(PasswordHashingService passwordHashingService) {
        this.passwordHashingService = passwordHashingService;
    }

    @Override
    public String initiateDeviceRegistration(JWK publicKey, DeviceRegInitRequest regInitRequest) {
        return generateNonce();
    }

    @Override
    public String validateDeviceRegistration(JWK devicePub, DeviceValidateRequest deviceValidateRequest) throws IOException {
        String initiationNonce = deviceValidateRequest.getInitiationNonce();
        String powNonce = deviceValidateRequest.getPowNonce();
        String newPowHash = deviceValidateRequest.getPowHash();

        // Make a JSON object out of initiationNonce, devicePub, powNonce
        String powJSON = "{\"initiationNonce\":\"" + initiationNonce + "\",\"devicePub\":" + devicePub.toJSONString() + ",\"powNonce\":\"" + powNonce + "\"}";
        JsonCanonicalizer jc = new JsonCanonicalizer(powJSON);
        String hashInput = jc.getEncodedString();

        // Extract timestamp from nonce and verify it's within allowed window (e.g., 15 minutes)
        LocalDateTime currentTime = LocalDateTime.now();
        
        try {
            // We need to validate the timestamp encoded in the nonce
            // For each 15-minute window, we'll check if the nonce could have been generated in that window
            boolean validNonce = false;
            
            // Check current and previous time window
            LocalDateTime timeToCheck = currentTime.minusMinutes(0);
            int flattenedMinute = timeToCheck.getMinute() / 15 * 15;
            LocalDateTime flattenedTimestamp = timeToCheck.withMinute(flattenedMinute).withSecond(0).withNano(0);
            String timestampString = flattenedTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Check if the provided initiationNonce verifies against this timestamp
            if (passwordHashingService.verify(timestampString, initiationNonce)) {
                validNonce = true;
            }
            
            if (!validNonce) {
                return "Error: Registration time elapsed, please try again";
            }
        } catch (Exception e) {
            logger.error("Error validating nonce timestamp", e);
            return "Error: Unable to validate registration time";
        }
        
        try {
            // Calculate the actual SHA-256 hash for PoW verification
            String calculatedHash = calculateSHA256(hashInput);
            
            // Log both hashes to help diagnose the issue
            logger.info("PoW Verification - Input JSON: {}", powJSON);
            logger.info("PoW Verification - Calculated Hash: {}", calculatedHash);
            logger.info("PoW Verification - Provided Hash: {}", newPowHash);
            
            if (!calculatedHash.equals(newPowHash)) {
                logger.warn("PoW Verification Failed - Hashes don't match");
                return "Error: Verification of PoW failed";
            }
            
            logger.info("PoW Verification Successful");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error calculating SHA-256 hash", e);
            return "Error: Unable to verify proof of work";
        }

        JsonCanonicalizer pubJc = new JsonCanonicalizer(devicePub.toJSONString());
        String devicePublicKey = pubJc.getEncodedString();
        logger.warn(devicePublicKey);
        return generateDeviceCertificate(devicePublicKey);
    }
    /**
     * Calculates an actual SHA-256 hash of the input and returns it as a hex string
     * Used for deterministic hashing needed in Proof of Work verification
     * This matches the frontend's CryptoJS.SHA256().toString(CryptoJS.enc.Hex) approach
     */
    String calculateSHA256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        
        // Convert byte array to hex string to match frontend's encoding
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String generateNonce() {
        LocalDateTime timestamp = LocalDateTime.now();

        // Flatten the timestamp to the nearest previous 15-minute interval
        int flattenedMinute = timestamp.getMinute() / 15 * 15;

        LocalDateTime flattenedTimestamp = timestamp.withMinute(flattenedMinute).withSecond(0).withNano(0);

        String timestampString = flattenedTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Use the PasswordHashingService to generate a secure hash
        return passwordHashingService.hash(timestampString);
    }

    String generateDeviceCertificate(String deviceJwkJson) {
        try {
            // Parse the server's private key from the JWK JSON string
            ECKey serverPrivateKey = (ECKey) JWK.parse(SERVER_PRIVATE_KEY_JSON);
            if (serverPrivateKey.getD() == null) {
                throw new IllegalStateException("Private key 'd' (private) parameter is missing.");
            }

            // Signer using server's private key
            JWSSigner signer = new ECDSASigner(serverPrivateKey);

            // Parse the server's public key
            ECKey serverPublicKey = (ECKey) JWK.parse(SERVER_PUBLIC_KEY_JSON);

            // Compute SHA-256 hash of the server’s public JWK to use as `kid`
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(serverPublicKey.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8));
            String kid = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

            // Create JWT Header
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(kid) // Set 'kid' as the SHA-256 of server public JWK
                    .type(JOSEObjectType.JWT)
                    .build();

            // Parse device's public JWK
            JWK deviceJwk = JWK.parse(deviceJwkJson);

            // Create JWT Payload
            long issuedAt = System.currentTimeMillis() / 60000; // Convert to seconds

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

            String dev = signedJWT.serialize();
            logger.info(dev);
            return dev;

        } catch (Exception e) {
            throw new IllegalStateException("Error generating device certificate", e);
        }
    }

}
