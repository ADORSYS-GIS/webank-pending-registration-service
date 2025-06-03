package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {
    @Mock
    private OtpRequestRepository otpRequestRepository;

    @InjectMocks
    private OtpServiceImpl otpService;

    private static final String TEST_PHONE_NUMBER = "+1234567890";
    private static final String TEST_OTP = "12345";
    private static final String TEST_PUBLIC_KEY = "test-public-key";
    private static final String TEST_PUBLIC_KEY_HASH = "test-public-key-hash";
    private static final String TEST_OTP_HASH = "test-otp-hash";

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(otpRequestRepository);
    }

    @Test
    void generateOtp_shouldGenerate5DigitNumber() {
        // Act
        String otp = otpService.generateOtp();

        // Assert
        assertNotNull(otp);
        assertEquals(5, otp.length());
        assertTrue(Integer.parseInt(otp) >= 10000 && Integer.parseInt(otp) <= 99999);
    }

    @Test
    void sendOtp_withInvalidPhoneNumber_shouldThrowException() {
        // Arrange
        String invalidPhoneNumber = "invalid";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            otpService.sendOtp(invalidPhoneNumber);
        });
    }
}