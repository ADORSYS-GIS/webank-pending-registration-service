package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.service.DeviceRegServiceApi;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import java.text.ParseException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.adorsys.webank.security.HashHelper;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceRegServiceImpl implements DeviceRegServiceApi {
    private final PasswordHashingService passwordHashingService;
    private final HashHelper hashHelper;

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
        return generateNonce();
    }

    @Override
    public String validateDeviceRegistration(JWK devicePub, DeviceValidateRequest deviceValidateRequest) throws IOException {
        // Extract request parameters
        String initiationNonce = deviceValidateRequest.getInitiationNonce();
        String powNonce = deviceValidateRequest.getPowNonce();
        String newPowHash = deviceValidateRequest.getPowHash();

        // Step 1: Validate the nonce timestamp
        String nonceValidationError = validateNonceTimestamp(initiationNonce);
        if (nonceValidationError != null) {
            return nonceValidationError;
        }
        
        // Step 2: Create and canonicalize the PoW JSON
        String powJSON = String.format(
                "{\"initiationNonce\":\"%s\",\"devicePub\":%s,\"powNonce\":\"%s\"}", 
                initiationNonce, devicePub.toJSONString(), powNonce);
        String hashInput = new JsonCanonicalizer(powJSON).getEncodedString();
        
        // Step 3: Verify the proof of work
        String powValidationError = validateProofOfWork(hashInput, powJSON, newPowHash);
        if (powValidationError != null) {
            return powValidationError;
        }
        
        // Step 4: Generate the device certificate
        String devicePublicKey = new JsonCanonicalizer(devicePub.toJSONString()).getEncodedString();
        return generateDeviceCertificate(devicePublicKey);
    }
    
    /**
     * Validates if the nonce timestamp is within the acceptable time window
     * @param initiationNonce The nonce to validate
     * @return Error message if validation fails, null if validation succeeds
     */
    private String validateNonceTimestamp(String initiationNonce) {
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime timeToCheck = currentTime.minusMinutes(0);
            int flattenedMinute = timeToCheck.getMinute() / 15 * 15;
            LocalDateTime flattenedTimestamp = timeToCheck.withMinute(flattenedMinute).withSecond(0).withNano(0);
            String timestampString = flattenedTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            if (!passwordHashingService.verify(timestampString, initiationNonce)) {
                log.warn("Nonce validation failed: Registration time elapsed");
                return "Error: Registration time elapsed, please try again";
            }
            return null;
        } catch (Exception e) {
            log.error("Error validating nonce timestamp", e);
            return "Error: Unable to validate registration time";
        }
    }
    
    /**
     * Validates the proof of work hash
     * @param hashInput Canonicalized input for hashing
     * @param powJSON Original JSON for logging purposes
     * @param providedHash Hash provided by the client
     * @return Error message if validation fails, null if validation succeeds
     */
    private String validateProofOfWork(String hashInput, String powJSON, String providedHash) {
        try {
            String calculatedHash = hashHelper.calculateSHA256AsHex(hashInput);
            log.debug("Calculated hash: {}", calculatedHash);
            log.debug("Provided hash: {}", providedHash);
            log.debug("PoW Verification - Input JSON: {}", powJSON);
            log.debug("PoW Verification - Provided Hash: {}", providedHash);
            
            if (!calculatedHash.equals(providedHash)) {
                log.warn("PoW Verification Failed - Hash mismatch");
                return "Error: Verification of PoW failed";
            }
            return null;
        } catch (Exception e) {
            log.error("Error calculating SHA-256 hash", e);
            return "Error: Unable to verify proof of work";
        }
    }
    /**
     * Calculates an actual SHA-256 hash of the input and returns it as a hex string
     * Used for deterministic hashing needed in Proof of Work verification
     * This matches the frontend's CryptoJS.SHA256().toString(CryptoJS.enc.Hex) approach
     * @deprecated Use hashHelper.calculateSHA256AsHex instead
     */
    @Deprecated
    String calculateSHA256(String input) throws NoSuchAlgorithmException {
        return hashHelper.calculateSHA256AsHex(input);
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

    /**
     * Generates a signed JWT certificate for a device
     * @param deviceJwkJson The device's public key in JWK format
     * @return A signed JWT containing the device certificate
     */
    String generateDeviceCertificate(String deviceJwkJson) {
        try {
            log.debug("Generating device certificate");
            
            // Step 1: Parse server keys and create signer
            ECKey serverPrivateKey = parseServerPrivateKey();
            JWSSigner signer = new ECDSASigner(serverPrivateKey);
            
            // Step 2: Generate key ID and create JWT header
            String kid = generateKeyId();
            JWSHeader header = createJwtHeader(kid);
            
            // Step 3: Parse device public key and create claims
            JWK deviceJwk = JWK.parse(deviceJwkJson);
            JWTClaimsSet claimsSet = createJwtClaims(deviceJwk);
            
            // Step 4: Sign and serialize the JWT
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);
            
            String jwt = signedJWT.serialize();
            log.debug("Device certificate generated successfully");
            return jwt;
            
        } catch (Exception e) {
            log.error("Failed to generate device certificate", e);
            throw new IllegalStateException("Error generating device certificate", e);
        }
    }
    
    /**
     * Parses the server's private key from the JWK JSON string
     * @return The server's ECKey private key
     * @throws ParseException If the key cannot be parsed
     */
    private ECKey parseServerPrivateKey() throws ParseException {
        ECKey serverPrivateKey = (ECKey) JWK.parse(SERVER_PRIVATE_KEY_JSON);
        if (serverPrivateKey.getD() == null) {
            log.error("Server private key is missing 'd' parameter");
            throw new IllegalStateException("Private key 'd' (private) parameter is missing.");
        }
        return serverPrivateKey;
    }
    
    /**
     * Generates a key ID by hashing the server's public key
     * @return Base64URL-encoded key ID
     * @throws ParseException If the key cannot be parsed
     * @throws NoSuchAlgorithmException If SHA-256 algorithm is not available
     */
    private String generateKeyId() throws ParseException {
        ECKey serverPublicKey = (ECKey) JWK.parse(SERVER_PUBLIC_KEY_JSON);
        return hashHelper.computeKeyId(serverPublicKey);
    }
    
    /**
     * Creates a JWT header with the specified key ID
     * @param kid The key ID to include in the header
     * @return A JWSHeader object
     */
    private JWSHeader createJwtHeader(String kid) {
        return new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(kid)
                .type(JOSEObjectType.JWT)
                .build();
    }
    
    /**
     * Creates JWT claims using the device's public key
     * @param deviceJwk The device's public key
     * @return A JWTClaimsSet object
     */
    private JWTClaimsSet createJwtClaims(JWK deviceJwk) {
        long issuedAt = System.currentTimeMillis() / 60000; // Convert to seconds
        
        return new JWTClaimsSet.Builder()
                .issuer(issuer)  // Use the configured issuer from properties
                .audience(deviceJwk.getKeyID()) // Use device public key ID as audience
                .claim("cnf", Collections.singletonMap("jwk", deviceJwk.toJSONObject()))
                .issueTime(new Date(issuedAt * 1000))
                .expirationTime(new Date((issuedAt + (expirationTimeMs / 1000)) * 1000))
                .build();
    }

}
