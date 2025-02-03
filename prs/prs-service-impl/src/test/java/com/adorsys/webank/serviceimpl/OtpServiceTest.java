package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.nimbusds.jose.jwk.JWK;
import com.twilio.*;
import com.twilio.rest.api.v2010.account.*;
import com.twilio.type.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.test.util.*;

import java.nio.charset.*;
import java.security.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

   OtpServiceImpl otpServiceImpl = new OtpServiceImpl();

   @InjectMocks
   private OtpServiceImpl otpService;

    @Test
   void generateFiveDigitOtp() {
       String otp = otpServiceImpl.generateOtp();

       assertNotNull(otp, "Otp should not be null");
       assert otp.length() == 5 : "Otp should be four digits";
       assert otp.matches("\\d+") : "Otp should only contain digits";
       assert Integer.parseInt(otp) >= 10000 && Integer.parseInt(otp) <= 99999 : "Otp should be between 10000 and 99999";
   }

   @BeforeEach
   void setup() {
       // Inject test values for Twilio credentials
       ReflectionTestUtils.setField(otpService, "accountSid", "testAccountSid");
       ReflectionTestUtils.setField(otpService, "authToken", "testAuthToken");
       ReflectionTestUtils.setField(otpService, "fromPhoneNumber", "+1234567890");
       ReflectionTestUtils.setField(otpService, "salt", "testSalt");
       Twilio.init("testAccountSid", "testAuthToken");
   }

   @Test
   void testTwilioConnection() {
       assertDoesNotThrow(() -> Twilio.init("testAccountSid", "testAuthToken"));
   }

    @Test
    void testSendOtp_FailedToSend() {
        JWK mockJwk = mock(JWK.class);
        when(mockJwk.toJSONString()).thenReturn("{}" );

        mockStatic(Message.class);
        when(Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), (String) any())).thenThrow(new RuntimeException("Twilio error"));

        assertThrows(FailedToSendOTPException.class, () -> otpService.sendOtp(mockJwk, "+1234567890"));
    }

    @Test
    void testComputeHash_ValidInput() throws NoSuchAlgorithmException {
        String input = "test-input";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] expectedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        String expectedHashBase64 = Base64.getEncoder().encodeToString(expectedHash);

        String actualHash = otpService.computeHash(input);
        assertEquals(expectedHashBase64, actualHash);
    }

    @Test
    void testComputeHash_EmptyInput() throws NoSuchAlgorithmException {
        String input = "";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] expectedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        String expectedHashBase64 = Base64.getEncoder().encodeToString(expectedHash);

        String actualHash = otpService.computeHash(input);
        assertEquals(expectedHashBase64, actualHash);
    }
   @Test
   void testComputeHashWithEmptyInputs() {
       String otp = "";

       String actualHash = otpServiceImpl.computeHash(otp);

       assertNotNull(actualHash, "Hash should not be null");
       assertFalse(actualHash.isEmpty(), "Hash should not be empty");
   }
   @Test
    void phoneNumberCertGeneration() {}
}
