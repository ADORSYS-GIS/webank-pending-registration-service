package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.service.OtpServiceApi;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class OtpServiceImpl implements OtpServiceApi {

    @Override
    public String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int otp = 1000 + secureRandom.nextInt(9000);
        return String.valueOf(otp);
    }

    @Override
     public String sendOtp(String phoneNumber, String publicKey) {
        String otp = generateOtp();

        String salt = "5";

        String otpHash = computeHash(otp, phoneNumber,publicKey, salt);

     return otpHash + ":" + otp;
    }


    @Override
    public boolean validateOtp(String phoneNumber, String otp) {
        return false;
    }

    @Override
    public String computeHash(String otp, String phoneNumber, String publicKey, String salt) {
        try {
            String input = otp + phoneNumber + publicKey + salt;

            // Compute SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert hash to Base64
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error computing hash", e);
        }
    }
}
