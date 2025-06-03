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
    private static final Logger log = LoggerFactory.getLogger(DeviceRegServiceImpl.class);

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

    @Override
    public String initiateDeviceRegistration(JWK publicKey, DeviceRegInitRequest regInitRequest) {
        log.info("Initiating device registration");
        log.debug("Using public key with ID: {}", maskKeyId(publicKey.getKeyID()));
        
        String nonce = generateNonce(salt);
        log.info("Device registration initiated successfully");
        return nonce;
    }

    @Override
    public String validateDeviceRegistration(JWK devicePub, DeviceValidateRequest deviceValidateRequest) throws IOException {
        log.info("Validating device registration");
        
        String initiationNonce = deviceValidateRequest.getInitiationNonce();
        String nonce = generateNonce(salt);
        String powNonce = deviceValidateRequest.getPowNonce();
        String newPowHash = deviceValidateRequest.getPowHash();
        String powHash;
        
        log.debug("Validating with initiation nonce: {}, device public key ID: {}", 
                maskData(initiationNonce), maskKeyId(devicePub.getKeyID()));
        
        try {
            // Make a JSON object out of initiationNonce, devicePub, powNonce
            log.debug("Constructing proof of work JSON for validation");
            String powJSON = "{\"initiationNonce\":\"" + initiationNonce + "\",\"devicePub\":" + devicePub.toJSONString() + ",\"powNonce\":\"" + powNonce + "\"}";
            JsonCanonicalizer jc = new JsonCanonicalizer(powJSON);
            String hashInput = jc.getEncodedString();
            powHash = calculateSHA256(hashInput);
            log.debug("Calculated proof of work hash for validation");

            if (!initiationNonce.equals(nonce)) {
                log.warn("Registration time elapsed, verification failed");
                return "Error: Registration time elapsed, please try again";
            } else if (!powHash.equals(newPowHash)) {
                log.warn("Verification of proof of work failed");
                return "Error: Verification of PoW failed";
            }
            
            log.debug("Proof of work validation successful");

        } catch (NoSuchAlgorithmException e) {
            log.error("Error calculating SHA-256 hash for device validation", e);
            return "Error: Unable to hash the parameters";
        }
        
        log.debug("Canonicalizing device public key");
        JsonCanonicalizer jc = new JsonCanonicalizer(devicePub.toJSONString());
        String devicePublicKey = jc.getEncodedString();
        
        log.info("Device validation successful, generating certificate");
        return generateDeviceCertificate(devicePublicKey);
    }
    
    String calculateSHA256(String input) throws NoSuchAlgorithmException {
        log.debug("Calculating SHA-256 hash");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        // Convert byte array to hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        log.debug("SHA-256 hash calculation completed");
        return hexString.toString();
    }

    public static String generateNonce(String salt) {
        if (salt == null) {
            LoggerFactory.getLogger(DeviceRegServiceImpl.class).error("Salt cannot be null when generating nonce");
            throw new HashComputationException("Salt cannot be null");
        }
        
        LocalDateTime timestamp = LocalDateTime.now();

        // Flatten the timestamp to the nearest previous 15-minute interval
        int flattenedMinute = (timestamp.getMinute() / 15) * 15;
        LocalDateTime flattenedTimestamp = timestamp.withMinute(flattenedMinute).withSecond(0).withNano(0);

        try {
            String timestampString = flattenedTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String input = timestampString + salt;

            // Compute SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert hash to Base64
            String nonce = Base64.getEncoder().encodeToString(hashBytes);
            LoggerFactory.getLogger(DeviceRegServiceImpl.class).debug("Generated nonce for timestamp: {}", flattenedTimestamp);
            return nonce;
        } catch (NoSuchAlgorithmException e) {
            LoggerFactory.getLogger(DeviceRegServiceImpl.class).error("Error computing hash for nonce generation", e);
            throw new HashComputationException("Error computing hash");
        }
    }

    String generateDeviceCertificate(String deviceJwkJson) {
        log.info("Generating device certificate");
        
        try {
            // Parse the server's private key from the JWK JSON string
            log.debug("Parsing server private key");
            ECKey serverPrivateKey = (ECKey) JWK.parse(SERVER_PRIVATE_KEY_JSON);
            if (serverPrivateKey.getD() == null) {
                log.error("Private key 'd' parameter is missing in server key");
                throw new IllegalStateException("Private key 'd' (private) parameter is missing.");
            }

            // Signer using server's private key
            log.debug("Creating JWT signer with server private key");
            JWSSigner signer = new ECDSASigner(serverPrivateKey);

            // Parse the server's public key
            log.debug("Parsing server public key");
            ECKey serverPublicKey = (ECKey) JWK.parse(SERVER_PUBLIC_KEY_JSON);

            // Compute SHA-256 hash of the server's public JWK to use as `kid`
            log.debug("Computing key ID (kid) from server public key");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(serverPublicKey.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8));
            String kid = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

            // Create JWT Header
            log.debug("Creating JWT header with kid: {}", maskData(kid));
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(kid) // Set 'kid' as the SHA-256 of server public JWK
                    .type(JOSEObjectType.JWT)
                    .build();

            // Parse device's public JWK
            log.debug("Parsing device public key");
            JWK deviceJwk = JWK.parse(deviceJwkJson);

            // Create JWT Payload
            long issuedAt = System.currentTimeMillis() / 60000; // Convert to seconds
            long expirationTime = issuedAt + (expirationTimeMs / 1000);
            
            log.debug("Creating JWT claims with issuer: {}, expiration: {} seconds", 
                     issuer, expirationTimeMs/1000);
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer("https://webank.com")  // Fixed issuer format
                    .audience(deviceJwk.getKeyID()) // Use device public key ID as audience
                    .claim("cnf", Collections.singletonMap("jwk", deviceJwk.toJSONObject())) // Fix JSON structure
                    .issueTime(new Date(issuedAt * 1000))
                    .expirationTime(new Date(expirationTime * 1000)) // Convert to milliseconds
                    .build();

            // Create JWT token
            log.debug("Signing JWT");
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);

            String certificate = signedJWT.serialize();
            log.info("Device certificate generated successfully, expires in {} seconds", expirationTimeMs/1000);
            
            if (log.isTraceEnabled()) {
                log.trace("Certificate: {}", certificate);
            }
            
            return certificate;

        } catch (Exception e) {
            log.error("Error generating device certificate", e);
            throw new IllegalStateException("Error generating device certificate", e);
        }
    }
    
    /**
     * Masks a key ID for logging purposes
     */
    private String maskKeyId(String keyId) {
        if (keyId == null || keyId.length() < 5) {
            return "********";
        }
        return keyId.substring(0, 2) + "****" + keyId.substring(keyId.length() - 2);
    }
    
    /**
     * Masks general sensitive data for logging
     */
    private String maskData(String data) {
        if (data == null || data.length() < 5) {
            return "********";
        }
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }
}
