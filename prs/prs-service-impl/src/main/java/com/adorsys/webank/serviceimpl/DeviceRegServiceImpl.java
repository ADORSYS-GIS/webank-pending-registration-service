package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.service.DeviceRegServiceApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class DeviceRegServiceImpl implements DeviceRegServiceApi {
    @Value("${otp.salt}")
    private String salt;

    @Override
    public String initiateDeviceRegistration(String jwtToken, DeviceRegInitRequest regInitRequest) {
        return generateNonce(salt);
    }

    @Override
    public String validateDeviceRegistration(String jwtToken, DeviceValidateRequest deviceValidateRequest) {
        String newNonce = deviceValidateRequest.getInitiationNonce();
        String nonce = generateNonce(salt);
        String powNonce = deviceValidateRequest.getPowNonce();
        String newPowHash = deviceValidateRequest.getPowHash();
        String pubKey = "publicKey";
        String powHash;
        try {
            String hashInput = newNonce + ":" + pubKey + ":" + powNonce;
            powHash = calculateSHA256(hashInput);

            if (!newNonce.equals(nonce)) {
                return "Error: Registration time elapsed, please try again";
            } else if (!powHash.equals(newPowHash)) {
                return "Error: Verification of PoW failed";
            }

        } catch (NoSuchAlgorithmException e) {
            return "Error: Unable to hash the parameters";

        }

        return "Successful validation";
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
}
