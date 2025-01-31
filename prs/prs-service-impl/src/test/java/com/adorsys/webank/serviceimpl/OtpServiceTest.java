//package com.adorsys.webank.serviceimpl;
//
//import com.nimbusds.jose.jwk.JWK;
//import com.twilio.*;
//import com.twilio.rest.api.v2010.account.*;
//import com.twilio.type.*;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.*;
//import org.mockito.*;
//import org.mockito.junit.jupiter.*;
//import org.springframework.test.util.*;
//
//import java.nio.charset.*;
//import java.security.*;
//import java.text.ParseException;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//public class OtpServiceTest {
//
//    OtpServiceImpl otpServiceImpl = new OtpServiceImpl();
//
//    @InjectMocks
//    private OtpServiceImpl otpService;
//
//    @Mock
//    private Message message;
//
//    @Test
//    void generateFourDigitOtp() {
//        String otp = otpServiceImpl.generateOtp();
//
//        assertNotNull(otp, "Otp should not be null");
//        assert otp.length() == 4 : "Otp should be four digits";
//        assert otp.matches("\\d+") : "Otp should only contain digits";
//        assert Integer.parseInt(otp) >= 1000 && Integer.parseInt(otp) <= 9999 : "Otp should be between 1000 and 9999";
//    }
//
//    @BeforeEach
//    void setup() {
//        // Inject test values for Twilio credentials
//        ReflectionTestUtils.setField(otpService, "accountSid", "testAccountSid");
//        ReflectionTestUtils.setField(otpService, "authToken", "testAuthToken");
//        ReflectionTestUtils.setField(otpService, "fromPhoneNumber", "+1234567890");
//        ReflectionTestUtils.setField(otpService, "salt", "testSalt");
//        Twilio.init("testAccountSid", "testAuthToken");
//    }
//
//    @Test
//    void testTwilioConnection() {
//        assertDoesNotThrow(() -> Twilio.init("testAccountSid", "testAuthToken"));
//    }
//
//    @Test
//    void testSendOtpSuccessfully() throws ParseException {
//        // Mock the MessageCreator and Message
//        Message mockMessage = Mockito.mock(Message.class);
//        MessageCreator mockMessageCreator = Mockito.mock(MessageCreator.class);
//
//        // Mock the behavior of create() to return a mock Message
//        Mockito.when(mockMessageCreator.create()).thenReturn(mockMessage);
//
//        // Mock the behavior of Message.creator to return the mock MessageCreator
//        Mockito.mockStatic(Message.class).when(() ->
//                Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), any(String.class))
//        ).thenReturn(mockMessageCreator);
//
//        String phoneNumber = "+1234567890";
//        String publicKey = "testPublicKey";
//
//        // Call the method
//        String otpHash = otpService.sendOtp(JWK.parse(publicKey), phoneNumber);
//
//        // Assert the OTP hash is not null
//        assertNotNull(otpHash, "OTP hash should not be null");
//
//        // Verify interactions with the mock MessageCreator
//        Mockito.verify(mockMessageCreator, Mockito.times(1)).create();
//
//        // Update the assertion to check Base64 format
//        assertTrue(otpHash.matches("[a-zA-Z0-9+/=]+"), "OTP hash should be a valid Base64 string");
//    }
//
//    @Test
//    void testComputeHashWithValidInputs() throws NoSuchAlgorithmException {
//        String otp = "1234";
//        String phoneNumber = "+237654066316";
//        String publicKey = "public-key-123";
//        String salt = "unique-salt";
//
//        // Expected hash computation
//        String input = otp + phoneNumber + publicKey + salt;
//        MessageDigest digest = MessageDigest.getInstance("SHA-256");
//        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
//        String expectedHash = Base64.getEncoder().encodeToString(hashBytes);
//
//        // Compute hash using the method
//        String actualHash = otpServiceImpl.computeHash(otp);
//
//        assertNotNull(actualHash, "Hash should not be null");
//        assertEquals(expectedHash, actualHash, "Hashes should match");
//    }
//
//    @Test
//    void testComputeHashWithEmptyInputs() {
//        String otp = "";
//        String phoneNumber = "";
//        String publicKey = "";
//        String salt = "";
//
//        String actualHash = otpServiceImpl.computeHash(otp);
//
//        assertNotNull(actualHash, "Hash should not be null");
//        assertFalse(actualHash.isEmpty(), "Hash should not be empty");
//    }
//}
