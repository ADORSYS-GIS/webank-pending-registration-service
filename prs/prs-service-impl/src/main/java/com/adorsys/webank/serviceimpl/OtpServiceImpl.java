package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.service.OtpServiceApi;
import org.springframework.stereotype.Service;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import com.twilio.type.PhoneNumber;


@Service
public class OtpServiceImpl implements OtpServiceApi {

    // Twilio credentials
    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    @Value("${otp.salt}")
    private String salt;

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken); // Initialize Twilio once
    }

    @Override
    public String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int otp = 1000 + secureRandom.nextInt(9000);
        return String.valueOf(otp);
    }

    @Override
    public String sendOtp(String phoneNumber, String publicKey) {
        if (phoneNumber == null || !phoneNumber.matches("\\+?[1-9]\\d{1,14}")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        try {
            String otp = generateOtp();
            String otpHash = computeHash(otp, phoneNumber, publicKey, salt);

            // Send OTP via Twilio
            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    "Your OTP is: " + otp
            ).create();

            return otpHash;

        } catch (Exception e) {
            throw new FailedToSendOTPException("Failed to send OTP");
        }
    }

    @Override
    public boolean validateOtp(String phoneNumber, String publicKey, String otpInput, String otpHash) {
        try {
            String newOtpHash = computeHash(otpInput, phoneNumber, publicKey, salt);
            return newOtpHash.equals(otpHash);
        } catch (Exception e) {
            return false;
        }
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
            throw new HashComputationException("Error computing hash");
        }
    }
}
