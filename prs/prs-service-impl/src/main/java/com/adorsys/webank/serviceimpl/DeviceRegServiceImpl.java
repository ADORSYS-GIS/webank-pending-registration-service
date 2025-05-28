
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
import java.text.ParseException;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

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

    @Override
    public String initiateDeviceRegistration( DeviceRegInitRequest regInitRequest) {
        return generateNonce(salt);
    }

    @Override
    public String validateDeviceRegistration(DeviceValidateRequest deviceValidateRequest) throws IOException, ParseException {
        String initiationNonce = deviceValidateRequest.getInitiationNonce();
        String nonce = generateNonce(salt);
        String powNonce = deviceValidateRequest.getPowNonce();
        String newPowHash = deviceValidateRequest.getPowHash();
        String powHash;


        ECKey  devicePub = ECKey.parse(SecurityUtils.extractDeviceJwkFromContext());
        try {
            // Make a JSON object out of initiationNonce, devicePub, powNonce
            String powJSON = "{\"initiationNonce\":\"" + initiationNonce + "\",\"devicePub\":" + devicePub.toJSONString() + ",\"powNonce\":\"" + powNonce + "\"}";
            JsonCanonicalizer jc = new JsonCanonicalizer(powJSON);
            String hashInput = jc.getEncodedString();
            powHash = calculateSHA256(hashInput);

            if (!initiationNonce.equals(nonce)) {
                return "Error: Registration time elapsed, please try again";
            } else if (!powHash.equals(newPowHash)) {
                return "Error: Verification of PoW failed";
            }

        } catch (NoSuchAlgorithmException e) {
            logger.error("Error calculating SHA-256 hash", e);
            return "Error: Unable to hash the parameters";
        }
        JsonCanonicalizer jc = new JsonCanonicalizer(devicePub.toJSONString());
        String devicePublicKey = jc.getEncodedString();
        logger.warn(devicePublicKey);
        return generateDeviceCertificate(devicePublicKey);
    }
    String calculateSHA256(String input) throws NoSuchAlgorithmException {
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
        return hexString.toString();
    }

    public static String generateNonce(String salt) {
        if (salt == null) {
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
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new HashComputationException("Error computing hash");
        }
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
            logger.info(String.valueOf(issuedAt));
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
