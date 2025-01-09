package com.adorsys.webank.serviceimpl;
import static org.junit.jupiter.api.Assertions.*;
import  com.twilio.rest.api.v2010.account.MessageCreator;
import org.junit.jupiter.api.Test;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {


    OtpServiceImpl otpServiceImpl = new OtpServiceImpl();

    @InjectMocks
    private OtpServiceImpl otpService;

    @Mock
    private Message message;

    @Test
    void generateFourDigitOtp() {
        String otp = otpServiceImpl.generateOtp();

        assertNotNull(otp, "Otp should not be null");
        assert otp.length() == 4 : "Otp should be four digits";
        assert otp.matches("\\d+") : "Otp should only contain digits";
        assert Integer.parseInt(otp) >= 1000 && Integer.parseInt(otp) <= 9999 : "Otp should be between 1000 and 9999";
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
    void testSendOtpSuccessfully() {
        // Mock the MessageCreator and Message
        Message mockMessage = Mockito.mock(Message.class);
        MessageCreator mockMessageCreator = Mockito.mock(MessageCreator.class);

        // Mock the behavior of create() to return a mock Message
        Mockito.when(mockMessageCreator.create()).thenReturn(mockMessage);

        // Mock the behavior of Message.creator to return the mock MessageCreator
        Mockito.mockStatic(Message.class).when(() ->
                Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), any(String.class))
        ).thenReturn(mockMessageCreator);

        String phoneNumber = "+1234567890";
        String publicKey = "testPublicKey";

        // Call the method
        String otpHash = otpService.sendOtp(phoneNumber, publicKey);

        // Assert the OTP hash is not null
        assertNotNull(otpHash, "OTP hash should not be null");

        // Verify interactions with the mock MessageCreator
        Mockito.verify(mockMessageCreator, Mockito.times(1)).create();

        // Update the assertion to check Base64 format
        assertTrue(otpHash.matches("[a-zA-Z0-9+/=]+"), "OTP hash should be a valid Base64 string");
    }
}