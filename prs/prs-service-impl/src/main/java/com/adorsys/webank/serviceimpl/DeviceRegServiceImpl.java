package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.config.*;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.exceptions.*;
import com.adorsys.webank.service.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;
import org.erdtman.jcs.*;
import org.slf4j.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
@Service
public class DeviceRegServiceImpl implements DeviceRegServiceApi {
    private static final Logger log = LoggerFactory.getLogger(DeviceRegServiceImpl.class);

    @Value("${otp.salt}")
    private String salt;

    @Autowired
    private KeyLoader keyLoader;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration-time-ms}")
    private Long expirationTimeMs;

    @Override
    public String initiateDeviceRegistration(DeviceRegInitRequest regInitRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Initiating device registration [correlationId={}]", correlationId);
        
        String nonce = generateNonce(salt);
        log.info("Device registration initiated successfully [correlationId={}]", correlationId);
        return nonce;
    }

    @Override
    public String validateDeviceRegistration(DeviceValidateRequest deviceValidateRequest) throws IOException {
        String correlationId = MDC.get("correlationId");
        log.info("Validating device registration [correlationId={}]", correlationId);
        
        String initiationNonce = deviceValidateRequest.getInitiationNonce();
        String nonce = generateNonce(salt);
        String powNonce = deviceValidateRequest.getPowNonce();
        String newPowHash = deviceValidateRequest.getPowHash();
        String powHash;
        
        // Extract device public key from security context
        ECKey devicePub = SecurityUtils.extractDeviceJwkFromContext();
        
        log.debug("Validating with initiation nonce: {}, device public key ID: {} [correlationId={}]", 
                maskData(initiationNonce), maskKeyId(devicePub.getKeyID()), correlationId);
        
        try {
            // Make a JSON object out of initiationNonce, devicePub, powNonce
            log.debug("Constructing proof of work JSON for validation [correlationId={}]", correlationId);
            String powJSON = "{\"initiationNonce\":\"" + initiationNonce + "\",\"devicePub\":" + devicePub.toJSONString() + ",\"powNonce\":\"" + powNonce + "\"}";
            JsonCanonicalizer jc = new JsonCanonicalizer(powJSON);
            String hashInput = jc.getEncodedString();
            powHash = calculateSHA256(hashInput);
            log.debug("Calculated proof of work hash for validation [correlationId={}]", correlationId);

            if (!initiationNonce.equals(nonce)) {
                log.warn("Registration time elapsed, verification failed [correlationId={}]", correlationId);
                return "Error: Registration time elapsed, please try again";
            } else if (!powHash.equals(newPowHash)) {
                log.warn("Verification of proof of work failed [correlationId={}]", correlationId);
                return "Error: Verification of PoW failed";
            }
            
            log.debug("Proof of work validation successful [correlationId={}]", correlationId);

        } catch (NoSuchAlgorithmException e) {
            log.error("Error calculating SHA-256 hash for device validation [correlationId={}]", correlationId, e);
            return "Error: Unable to hash the parameters";
        }
        
        log.debug("Canonicalizing device public key [correlationId={}]", correlationId);
        JsonCanonicalizer jc = new JsonCanonicalizer(devicePub.toJSONString());
        String devicePublicKey = jc.getEncodedString();
        
        log.info("Device validation successful, generating certificate [correlationId={}]", correlationId);
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
