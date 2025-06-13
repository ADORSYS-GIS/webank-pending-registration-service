package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.adorsys.error.AccountNotFoundException;
import com.adorsys.error.FailedToSendOTPException;
import com.adorsys.error.HashComputationException;
import com.adorsys.error.ValidationException;
import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.dto.response.EmailResponse;
import com.adorsys.webank.dto.response.EmailValidationResponse;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.serviceimpl.helper.MailHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@ExtendWith(MockitoExtension.class)
class EmailOtpServiceImplTest {

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailHelper mailHelper;

    @InjectMocks
    private EmailOtpServiceImpl emailOtpService;

    private static final String TEST_ACCOUNT_ID = "test-account-id";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_OTP = "123456";
    private static final String TEST_OTP_HASH = "hashed-otp";
    private static final String TEST_JSON_DATA = "{\"key\":\"value\"}";

    @BeforeEach
    void setUp() throws Exception {
        reset(personalInfoRepository, objectMapper, passwordEncoder, mailHelper);
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn(TEST_JSON_DATA);
    }

    @Test
    void generateOtp_shouldGenerate6DigitNumber() {
        // Act
        String otp = emailOtpService.generateOtp();

        // Assert
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(Integer.parseInt(otp) >= 100000 && Integer.parseInt(otp) <= 999999);
    }

    @Test
    void sendEmailOtp_withInvalidEmail_shouldThrowValidationException() {
        // Arrange
        String invalidEmail = "invalid-email";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            emailOtpService.sendEmailOtp(TEST_ACCOUNT_ID, invalidEmail);
        });
        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    void sendEmailOtp_withNullAccountId_shouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            emailOtpService.sendEmailOtp(null, TEST_EMAIL);
        });
        assertEquals("Account ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void sendEmailOtp_withNonExistentAccount_shouldThrowAccountNotFoundException() {
        // Arrange
        when(personalInfoRepository.findById(TEST_ACCOUNT_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () -> {
            emailOtpService.sendEmailOtp(TEST_ACCOUNT_ID, TEST_EMAIL);
        });
        assertEquals("No user found for account: " + TEST_ACCOUNT_ID, exception.getMessage());
    }

    @Test
    void sendEmailOtp_success() throws JsonProcessingException {
        // Arrange
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        when(personalInfoRepository.findById(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(personalInfo));
        when(passwordEncoder.encode(any()))
            .thenReturn(TEST_OTP_HASH);
        doNothing().when(mailHelper).sendOtpEmail(any(), any());
        when(objectMapper.writeValueAsString(any())).thenReturn(TEST_JSON_DATA);

        // Act
        EmailResponse response = emailOtpService.sendEmailOtp(TEST_ACCOUNT_ID, TEST_EMAIL);

        // Assert
        assertNotNull(response);
        assertEquals(EmailResponse.EmailStatus.SUCCESS, response.getStatus());
        assertTrue(response.getMessage().contains("OTP sent successfully"));
        verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
        verify(mailHelper).sendOtpEmail(eq(TEST_EMAIL), any());
    }

    @Test
    void validateEmailOtp_withInvalidOtp_shouldReturnFailedResponse() {
        // Arrange
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setEmailOtpHash(TEST_OTP_HASH);
        personalInfo.setOtpExpirationDateTime(LocalDateTime.now().plusMinutes(5));
        when(personalInfoRepository.findById(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(personalInfo));
        when(passwordEncoder.matches(any(), any()))
            .thenReturn(false);

        // Act
        EmailValidationResponse response = emailOtpService.validateEmailOtp(TEST_EMAIL, "wrong-otp", TEST_ACCOUNT_ID);

        // Assert
        assertEquals(EmailValidationResponse.ValidationStatus.FAILED, response.getStatus());
        assertTrue(response.getMessage().contains("Invalid OTP provided"));
    }

    @Test
    void validateEmailOtp_withExpiredOtp_shouldReturnFailedResponse() {
        // Arrange
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setEmailOtpHash(TEST_OTP_HASH);
        personalInfo.setOtpExpirationDateTime(LocalDateTime.now().minusMinutes(1));
        when(personalInfoRepository.findById(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(personalInfo));

        // Act
        EmailValidationResponse response = emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, TEST_ACCOUNT_ID);

        // Assert
        assertEquals(EmailValidationResponse.ValidationStatus.FAILED, response.getStatus());
        assertTrue(response.getMessage().contains("OTP has expired"));
    }

    @Test
    void validateEmailOtp_success() {
        // Arrange
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setEmailOtpHash(TEST_OTP_HASH);
        personalInfo.setOtpExpirationDateTime(LocalDateTime.now().plusMinutes(5));
        when(personalInfoRepository.findById(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(personalInfo));
        when(passwordEncoder.matches(any(), any()))
            .thenReturn(true);

        // Act
        EmailValidationResponse response = emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, TEST_ACCOUNT_ID);

        // Assert
        assertEquals(EmailValidationResponse.ValidationStatus.SUCCESS, response.getStatus());
        assertTrue(response.getMessage().contains("OTP validated successfully"));
        verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
    }

    @Test
    void computeHash_shouldReturnValidHash() {
        // Arrange
        String input = "test-input";

        // Act
        String hash = emailOtpService.computeHash(input);

        // Assert
        assertNotNull(hash);
        assertTrue(hash.matches("^[a-f0-9]{64}$")); // SHA-256 hash is 64 hex characters
    }

    // @Test
    // void computeHash_withException_shouldThrowHashComputationException() throws JsonProcessingException {
    //     // Arrange
    //     JsonProcessingException testException = new JsonProcessingException("Test exception") {};
    //     when(objectMapper.writeValueAsString(any())).thenThrow(testException);

    //     // Act & Assert
    //     HashComputationException exception = assertThrows(HashComputationException.class, () -> {
    //         emailOtpService.computeOtpHash(TEST_OTP, TEST_ACCOUNT_ID);
    //     });
    //     assertEquals("Failed to compute OTP hash: " + testException.getMessage(), exception.getMessage());
    //     assertNotNull(exception.getCause());
    //     assertEquals("Test exception", exception.getCause().getMessage());
    // }
} 