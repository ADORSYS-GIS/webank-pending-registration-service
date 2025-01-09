package com.adorsys.webank.serviceimpl;
import static org.junit.jupiter.api.Assertions.*;

import com.adorsys.webank.exceptions.HashComputationException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class OtpServiceTest {


    OtpServiceImpl otpServiceImpl = new OtpServiceImpl();


    @Test
    void generateFourDigitOtp() {
        String otp = otpServiceImpl.generateOtp();

        assertNotNull(otp, "Otp should not be null");
        assert otp.length() == 4 : "Otp should be four digits";
        assert otp.matches("\\d+") : "Otp should only contain digits";
        assert Integer.parseInt(otp) >= 1000 && Integer.parseInt(otp) <= 9999 : "Otp should be between 1000 and 9999";
    }

    @Test
    void testComputeHashWithValidInputs() throws NoSuchAlgorithmException {
        String otp = "1234";
        String phoneNumber = "+237654066316";
        String publicKey = "public-key-123";
        String salt = "unique-salt";
        // Expected hash computation
        String input = otp + phoneNumber + publicKey + salt;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        String expectedHash = Base64.getEncoder().encodeToString(hashBytes);
        // Compute hash using the method
        String actualHash = otpServiceImpl.computeHash(otp, phoneNumber, publicKey, salt);
        assertNotNull(actualHash, "Hash should not be null");
        assertEquals(expectedHash, actualHash, "Hashes should match");
    }

    @Test
    void testComputeHashWithEmptyInputs() {
        String otp = "";
        String phoneNumber = "";
        String publicKey = "";
        String salt = "";
        String actualHash = otpServiceImpl.computeHash(otp, phoneNumber, publicKey, salt);
        assertNotNull(actualHash, "Hash should not be null");
        assertFalse(actualHash.isEmpty(), "Hash should not be empty");
    }


}
