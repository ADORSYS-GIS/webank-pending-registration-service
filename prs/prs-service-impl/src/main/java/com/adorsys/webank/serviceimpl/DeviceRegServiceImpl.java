package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.security.CertGeneratorHelper;
import com.adorsys.webank.service.DeviceRegServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.erdtman.jcs.JsonCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class DeviceRegServiceImpl implements DeviceRegServiceApi {
    private static final Logger logger = LoggerFactory.getLogger(DeviceRegServiceImpl.class);
    private final CertGeneratorHelper certGeneratorHelper;

    @Value("${otp.salt}")
    private String salt;

    public DeviceRegServiceImpl() {
        this.certGeneratorHelper = new CertGeneratorHelper();
    }

    @Override
    public String initiateDeviceRegistration(JWK publicKey, DeviceRegInitRequest regInitRequest) {
        return generateNonce(salt);
    }

    @Override
    public String validateDeviceRegistration(JWK devicePub, DeviceValidateRequest deviceValidateRequest) throws IOException {
        String initiationNonce = deviceValidateRequest.getInitiationNonce();
        String nonce = generateNonce(salt);
        String powNonce = deviceValidateRequest.getPowNonce();
        String newPowHash = deviceValidateRequest.getPowHash();
        String powHash;

        try {
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
        return certGeneratorHelper.generateCertificate(devicePublicKey);
    }

    String calculateSHA256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

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
        int flattenedMinute = (timestamp.getMinute() / 15) * 15;
        LocalDateTime flattenedTimestamp = timestamp.withMinute(flattenedMinute).withSecond(0).withNano(0);

        try {
            String timestampString = flattenedTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String input = timestampString + salt;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new HashComputationException("Error computing hash");
        }
    }
}
