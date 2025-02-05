package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.twilio.*;
import com.twilio.rest.api.v2010.account.*;
import com.twilio.type.*;
import org.erdtman.jcs.JsonCanonicalizer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.test.util.*;

import java.lang.reflect.Field;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    OtpServiceImpl otpServiceImpl = new OtpServiceImpl();

    @Spy
    private OtpServiceImpl otpService;

    private static final String phoneNumber = "+1236567890";
    private static final String devicePublicKey = "testPublicKey";
    private ECKey serverKeyPair;
    private ECKey deviceKeyPair;
    private String otpInput;
    private String salt;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        serverKeyPair = new ECKeyGenerator(Curve.P_256)
                .keyID("123")
                .generate();
        deviceKeyPair = new ECKeyGenerator(Curve.P_256)
                .keyID("device-key")
                .generate();


        // Initialize service and inject keys
        otpService = new OtpServiceImpl();
        injectField("SERVER_PRIVATE_KEY_JSON", serverKeyPair.toJSONString());
        injectField("SERVER_PUBLIC_KEY_JSON", serverKeyPair.toPublicJWK().toJSONString());
        // Inject test values for Twilio credentials
        ReflectionTestUtils.setField(otpService, "accountSid", "testAccountSid");
        ReflectionTestUtils.setField(otpService, "authToken", "testAuthToken");
        ReflectionTestUtils.setField(otpService, "fromPhoneNumber", "+1236567890");
        ReflectionTestUtils.setField(otpService, "salt", "testSalt");
        Twilio.init("testAccountSid", "testAuthToken");

        // Initialize test values

        otpInput = "12345";
        salt = "testSalt";
    }

    private void injectField(String fieldName, String value) throws NoSuchFieldException, IllegalAccessException {
        Field field = OtpServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(otpService, value);
    }

    @Test
    void generateFiveDigitOtp() {
        String otp = otpServiceImpl.generateOtp();

        assertNotNull(otp, "Otp should not be null");
        assert otp.length() == 5 : "Otp should be five digits";
        assert otp.matches("\\d+") : "Otp should only contain digits";
        assert Integer.parseInt(otp) >= 10000 && Integer.parseInt(otp) <= 99999 : "Otp should be between 10000 and 99999";
    }

    @Test
    void testTwilioConnection() {
        assertDoesNotThrow(() -> Twilio.init("testAccountSid", "testAuthToken"));
    }

    @Test
    void testSendOtp_FailedToSend() {
        JWK mockJwk = mock(JWK.class);
        when(mockJwk.toJSONString()).thenReturn("{}");

        mockStatic(Message.class);
        when(Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), (String) any())).thenThrow(new RuntimeException("Twilio error"));

        assertThrows(FailedToSendOTPException.class, () -> otpService.sendOtp(mockJwk, "+1236567890"));
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

}
