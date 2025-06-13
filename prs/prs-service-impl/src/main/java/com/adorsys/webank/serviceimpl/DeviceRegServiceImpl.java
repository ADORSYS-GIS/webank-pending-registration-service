package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.config.JwtUtils;

import com.adorsys.webank.config.KeyLoader;
import com.adorsys.webank.config.SecurityUtils;
import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.dto.response.DeviceResponse;
import com.adorsys.webank.dto.response.DeviceValidationResponse;
import com.adorsys.webank.model.ProofOfWorkData;
import com.adorsys.webank.service.DeviceRegServiceApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.erdtman.jcs.JsonCanonicalizer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;


@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceRegServiceImpl implements DeviceRegServiceApi {

    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final KeyLoader keyLoader;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-time-ms}")
    private Long expirationTimeMs;

    @Override
    public DeviceResponse initiateDeviceRegistration(DeviceRegInitRequest regInitRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Initiating device registration [correlationId={}]", correlationId);
        
        String nonce = generateNonce();
        log.info("Device registration initiated successfully [correlationId={}]", correlationId);
        DeviceResponse response = new DeviceResponse();
        response.setStatus(DeviceResponse.InitStatus.INITIALIZED);
        response.setTimestamp(java.time.LocalDateTime.now());
        response.setMessage("Device registration initialized. Use the following nonce for validation.");
        response.setNonce(nonce);
        return response;
    }

    @Override
    public DeviceValidationResponse validateDeviceRegistration(DeviceValidateRequest deviceValidateRequest) throws IOException {
        String correlationId = MDC.get("correlationId");
        log.info("Validating device registration [correlationId={}]", correlationId);
        
        String initiationNonce = deviceValidateRequest.getInitiationNonce();
        // Step 1: Validate the nonce timestamp
        String nonceValidationError = validateNonceTimestamp(initiationNonce);
        if (nonceValidationError != null) {
            DeviceValidationResponse response = new DeviceValidationResponse();
            response.setStatus(DeviceValidationResponse.ValidationStatus.FAILED);
            response.setTimestamp(java.time.LocalDateTime.now());
            response.setMessage(nonceValidationError);
            return response;
        }

        String powNonce = deviceValidateRequest.getPowNonce();
        String newPowHash = deviceValidateRequest.getPowHash();

        String powJSON;

        // Extract device public key from security context
        ECKey devicePub = SecurityUtils.extractDeviceJwkFromContext();

        // Step 2: Create and canonicalize the PoW JSON using ProofOfWorkData POJO
        ProofOfWorkData powData = ProofOfWorkData.create(initiationNonce, devicePub, powNonce);

        log.debug("Validating with initiation nonce: {}, device public key ID: {} [correlationId={}]", 
                maskData(initiationNonce), maskKeyId(devicePub.getKeyID()), correlationId);
        
        try {
            powJSON = objectMapper.writeValueAsString(powData);
            String hashInput = new JsonCanonicalizer(powJSON).getEncodedString();

            // Step 3: Verify the proof of work
            String powValidationError = validateProofOfWork(hashInput, powJSON, newPowHash);
            log.debug("Calculated proof of work hash for validation [correlationId={}]", correlationId);

            if (powValidationError != null) {
                DeviceValidationResponse response = new DeviceValidationResponse();
                response.setStatus(DeviceValidationResponse.ValidationStatus.FAILED);
                response.setTimestamp(java.time.LocalDateTime.now());
                response.setMessage(powValidationError);
                return response;
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PoW JSON", e);
            DeviceValidationResponse response = new DeviceValidationResponse();
            response.setStatus(DeviceValidationResponse.ValidationStatus.FAILED);
            response.setTimestamp(java.time.LocalDateTime.now());
            response.setMessage("Error processing proof of work");
            return response;
        }
        
        log.debug("Canonicalizing device public key [correlationId={}]", correlationId);
        JsonCanonicalizer jc = new JsonCanonicalizer(devicePub.toJSONString());
        String devicePublicKey = jc.getEncodedString();
        
        log.info("Device validation successful, generating certificate [correlationId={}]", correlationId);
        String certificate = generateDeviceCertificate(devicePublicKey);
        DeviceValidationResponse response = new DeviceValidationResponse();
        response.setStatus(DeviceValidationResponse.ValidationStatus.VALIDATED);
        response.setTimestamp(java.time.LocalDateTime.now());
        response.setCertificate(certificate);
        response.setMessage("Device validated successfully.");
        return response;
    }
    private String validateNonceTimestamp(String initiationNonce) {
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime timeToCheck = currentTime.minusMinutes(0);
            int flattenedMinute = timeToCheck.getMinute() / 15 * 15;
            LocalDateTime flattenedTimestamp = timeToCheck.withMinute(flattenedMinute).withSecond(0).withNano(0);
            String timestampString = flattenedTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            if (!passwordEncoder.matches(timestampString, initiationNonce)) {
                log.warn("Nonce validation failed: Registration time elapsed");
                return "Error: Registration time elapsed, please try again";
            }
            return null;
        } catch (Exception e) {
            log.error("Error validating nonce timestamp", e);
            return "Error: Unable to validate registration time";
        }
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

    public String generateNonce() {
        
        LocalDateTime timestamp = LocalDateTime.now();

        // Flatten the timestamp to the nearest previous 15-minute interval
        int flattenedMinute = (timestamp.getMinute() / 15) * 15;

        LocalDateTime flattenedTimestamp = timestamp.withMinute(flattenedMinute).withSecond(0).withNano(0);

        String timestampString = flattenedTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Use the PasswordEncoder to generate a secure hash
        return passwordEncoder.encode(timestampString);
    }

    private String validateProofOfWork(String hashInput, String powJSON, String providedHash) {
        try {
            String calculatedHash = calculateSHA256(hashInput);
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

    String generateDeviceCertificate(String deviceJwkJson) {
        log.info("Generating device certificate");
        
        try {
            // Load keys using KeyLoader
            ECKey serverPrivateKey = keyLoader.loadPrivateKey();
            ECKey serverPublicKey = keyLoader.loadPublicKey();
            JWSSigner signer = new ECDSASigner(serverPrivateKey);
            JWSHeader header = JwtUtils.createJwtHeader(serverPublicKey);

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
