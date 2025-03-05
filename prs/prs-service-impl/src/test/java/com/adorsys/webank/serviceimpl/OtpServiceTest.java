package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.exceptions.HashComputationException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.twilio.*;
import org.erdtman.jcs.JsonCanonicalizer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.test.util.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    OtpServiceImpl otpServiceImpl = new OtpServiceImpl();

    @Spy
    private OtpServiceImpl otpService;

    ECKey deviceKeyPair;
    ECKey serverKeyPair;

    @BeforeEach
    void setup() throws HashComputationException, JOSEException, NoSuchFieldException, IllegalAccessException {
        serverKeyPair = new ECKeyGenerator(Curve.P_256).keyID("123").generate();
        deviceKeyPair = new ECKeyGenerator(Curve.P_256).keyID("device-key").generate();

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

/*    @Test
    void testSendOtp_FailedToSend() {
       JWK mockJwk = mock(JWK.class);
       when(mockJwk.toJSONString()).thenReturn("{}");
       mockStatic(Message.class);
       when(Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), (String) any())).thenThrow(new RuntimeException("Twilio error"));
       assertThrows(FailedToSendOTPException.class, () -> otpService.sendOtp(mockJwk, "+1236567890"));
}
*/

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
    void validateOtp_ValidOtp_ReturnsCertificate() throws JOSEException, IOException {
        // Arrange
        String phoneNumber = "+123456789";
        deviceKeyPair = new ECKeyGenerator(Curve.P_256).keyID("device-key").generate();
        JWK devicePub = deviceKeyPair.toPublicJWK();
        String otpInput = "12345";

        // Construct JSON and compute hash
        String otpJSON = String.format("{\"otp\":\"%s\",\"devicePub\":%s,\"phoneNumber\":\"%s\",\"salt\":\"%s\"}",
                otpInput, devicePub.toJSONString(), phoneNumber, otpService.salt);
        JsonCanonicalizer jc = new JsonCanonicalizer(otpJSON);
        String canonicalized = jc.getEncodedString();
        String expectedOtpHash = otpService.computeHash(canonicalized);

        // Act
        String result = otpService.validateOtp(phoneNumber, devicePub, otpInput, expectedOtpHash);

        // Assert
        assertTrue(result.startsWith("Certificate generated: "));
    }

    @Test
    void validateOtp_InvalidOtp_ReturnsFailure() throws IOException {
        // Arrange
        String phoneNumber = "+123456789";
        JWK devicePub = deviceKeyPair.toPublicJWK();
        String validOtp = "12345";
        String invalidOtp = "67890";

        // Compute hash for valid OTP
        String validOtpJSON = String.format("{\"otp\":\"%s\",\"devicePub\":%s,\"phoneNumber\":\"%s\",\"salt\":\"%s\"}",
                validOtp, devicePub.toJSONString(), phoneNumber, otpService.salt);
        JsonCanonicalizer jcValid = new JsonCanonicalizer(validOtpJSON);
        String canonicalizedValid = jcValid.getEncodedString();
        String validHash = otpService.computeHash(canonicalizedValid);

        // Act: Use invalid OTP with valid hash (hash won't match)
        String result = otpService.validateOtp(phoneNumber, devicePub, invalidOtp, validHash);

        // Assert
        assertEquals("OTP validation failed", result);
    }

    @Test
    void validateOtp_IncorrectHash_ReturnsFailure() {
        // Arrange
        String phoneNumber = "+123456789";
        JWK devicePub = deviceKeyPair.toPublicJWK();
        String otpInput = "12345";
        String incorrectHash = "incorrectHash";

        // Act
        String result = otpService.validateOtp(phoneNumber, devicePub, otpInput, incorrectHash);

        // Assert
        assertEquals("OTP validation failed", result);
    }

    @Test
    void validateOtp_InvalidDevicePub_ReturnsError() {
        // Arrange
        String phoneNumber = "+123456789";
        JWK invalidDevicePub = null;
        String otpInput = "12345";
        String otpHash = "dummyHash";

        // Act
        String result = otpService.validateOtp(phoneNumber, invalidDevicePub, otpInput, otpHash);

        // Assert
        assertEquals("Error validating the OTP", result);
    }

}
