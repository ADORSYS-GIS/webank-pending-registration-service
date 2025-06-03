// package com.adorsys.webank.serviceimpl;

// import com.adorsys.webank.domain.PersonalInfoEntity;
// import com.adorsys.webank.exceptions.FailedToSendOTPException;
// import com.adorsys.webank.repository.PersonalInfoRepository;
// import com.adorsys.error.ValidationException;
// import jakarta.mail.MessagingException;
// import jakarta.mail.internet.MimeMessage;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.*;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.mail.javamail.JavaMailSender;
// import org.springframework.mail.javamail.MimeMessageHelper;
// import org.springframework.test.util.ReflectionTestUtils;

// import java.time.LocalDateTime;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class EmailOtpServiceImplTest {

//     @Mock
//     private PersonalInfoRepository personalInfoRepository;

//     @Mock
//     private JavaMailSender mailSender;

//     @InjectMocks
//     private EmailOtpServiceImpl emailOtpService;

//     private static final String TEST_EMAIL = "test@example.com";
//     private static final String TEST_ACCOUNT_ID = "test-account-id";
//     private static final String TEST_SALT = "test-salt";
//     private static final String TEST_OTP = "123456";
//     private static final String TEST_FROM_EMAIL = "no-reply@test.com";

//     @BeforeEach
//     void setUp() {
//         ReflectionTestUtils.setField(emailOtpService, "salt", TEST_SALT);
//         ReflectionTestUtils.setField(emailOtpService, "fromEmail", TEST_FROM_EMAIL);
        
//         // Mock MimeMessage and MimeMessageHelper
//         MimeMessage mimeMessage = mock(MimeMessage.class);
//         MimeMessageHelper helper = mock(MimeMessageHelper.class);
//         when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
//     }

//     @Test
//     void testGenerateOtp() {
//         // Act
//         String otp = emailOtpService.generateOtp();

//         // Assert
//         assertNotNull(otp);
//         assertEquals(6, otp.length());
//         assertTrue(otp.matches("\\d{6}"));
//     }

//     @Test
//     void testSendEmailOtp_Success() throws MessagingException {
//         // Arrange
//         when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());
//         doNothing().when(mailSender).send(any(MimeMessage.class));

//         // Act
//         String result = emailOtpService.sendEmailOtp(TEST_ACCOUNT_ID, TEST_EMAIL);

//         // Assert
//         assertEquals("Email OTP sent successfully", result);
//         verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
//         verify(mailSender).send(any(MimeMessage.class));
//     }

//     @Test
//     void testSendEmailOtp_UpdateExisting() throws MessagingException {
//         // Arrange
//         PersonalInfoEntity existingEntity = new PersonalInfoEntity();
//         existingEntity.setAccountId(TEST_ACCOUNT_ID);
//         when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(existingEntity));
//         doNothing().when(mailSender).send(any(MimeMessage.class));

//         // Act
//         String result = emailOtpService.sendEmailOtp(TEST_ACCOUNT_ID, TEST_EMAIL);

//         // Assert
//         assertEquals("Email OTP sent successfully", result);
//         verify(personalInfoRepository).save(existingEntity);
//         verify(mailSender).send(any(MimeMessage.class));
//     }

//     @Test
//     void testSendEmailOtp_NullAccountId() {
//         // Act & Assert
//         ValidationException exception = assertThrows(ValidationException.class, () ->
//             emailOtpService.sendEmailOtp(null, TEST_EMAIL)
//         );
//         assertEquals("Account ID is required", exception.getMessage());
//         verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
//         verify(mailSender, never()).send(any(MimeMessage.class));
//     }

//     @Test
//     void testSendEmailOtp_EmptyAccountId() {
//         // Act & Assert
//         ValidationException exception = assertThrows(ValidationException.class, () ->
//             emailOtpService.sendEmailOtp("", TEST_EMAIL)
//         );
//         assertEquals("Account ID is required", exception.getMessage());
//         verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
//         verify(mailSender, never()).send(any(MimeMessage.class));
//     }

//     @Test
//     void testSendEmailOtp_NullEmail() {
//         // Act & Assert
//         ValidationException exception = assertThrows(ValidationException.class, () ->
//             emailOtpService.sendEmailOtp(TEST_ACCOUNT_ID, null)
//         );
//         assertEquals("Email is required", exception.getMessage());
//         verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
//         verify(mailSender, never()).send(any(MimeMessage.class));
//     }

//     @Test
//     void testSendEmailOtp_EmptyEmail() {
//         // Act & Assert
//         ValidationException exception = assertThrows(ValidationException.class, () ->
//             emailOtpService.sendEmailOtp(TEST_ACCOUNT_ID, "")
//         );
//         assertEquals("Email is required", exception.getMessage());
//         verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
//         verify(mailSender, never()).send(any(MimeMessage.class));
//     }

//     @Test
//     void testSendEmailOtp_InvalidEmailFormat() {
//         // Act & Assert
//         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
//             emailOtpService.sendEmailOtp(TEST_ACCOUNT_ID, "invalid-email")
//         );
//         assertEquals("Invalid email format", exception.getMessage());
//         verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
//         verify(mailSender, never()).send(any(MimeMessage.class));
//     }

//     @Test
//     void testSendEmailOtp_MailSenderError() throws MessagingException {
//         // Arrange
//         when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());
//         doThrow(new MessagingException("Failed to send email")).when(mailSender).send(any(MimeMessage.class));

//         // Act & Assert
//         FailedToSendOTPException exception = assertThrows(FailedToSendOTPException.class, () ->
//             emailOtpService.sendEmailOtp(TEST_ACCOUNT_ID, TEST_EMAIL)
//         );
//         assertTrue(exception.getMessage().contains("Failed to send Webank email OTP"));
//         verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
//     }

//     @Test
//     void testValidateEmailOtp_Success() {
//         // Arrange
//         PersonalInfoEntity entity = new PersonalInfoEntity();
//         entity.setAccountId(TEST_ACCOUNT_ID);
//         entity.setEmailOtpCode(TEST_OTP);
//         entity.setEmailOtpHash(emailOtpService.computeOtpHash(TEST_OTP, TEST_ACCOUNT_ID));
//         entity.setOtpExpirationDateTime(LocalDateTime.now().plusMinutes(5));
        
//         when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(entity));
//         when(personalInfoRepository.save(any(PersonalInfoEntity.class))).thenReturn(entity);

//         // Act
//         String result = emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, TEST_ACCOUNT_ID);

//         // Assert
//         assertEquals("Email OTP validated successfully", result);
//         assertEquals(TEST_EMAIL, entity.getEmail());
//         verify(personalInfoRepository).save(entity);
//     }

//     @Test
//     void testValidateEmailOtp_NullEmail() {
//         // Act & Assert
//         ValidationException exception = assertThrows(ValidationException.class, () ->
//             emailOtpService.validateEmailOtp(null, TEST_OTP, TEST_ACCOUNT_ID)
//         );
//         assertEquals("Email is required", exception.getMessage());
//         verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
//     }

//     @Test
//     void testValidateEmailOtp_NullOtp() {
//         // Act & Assert
//         ValidationException exception = assertThrows(ValidationException.class, () ->
//             emailOtpService.validateEmailOtp(TEST_EMAIL, null, TEST_ACCOUNT_ID)
//         );
//         assertEquals("OTP is required", exception.getMessage());
//         verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
//     }

//     @Test
//     void testValidateEmailOtp_NullAccountId() {
//         // Act & Assert
//         ValidationException exception = assertThrows(ValidationException.class, () ->
//             emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, null)
//         );
//         assertEquals("Account ID is required", exception.getMessage());
//         verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
//     }

//     @Test
//     void testValidateEmailOtp_ExpiredOtp() {
//         // Arrange
//         PersonalInfoEntity entity = new PersonalInfoEntity();
//         entity.setAccountId(TEST_ACCOUNT_ID);
//         entity.setOtpExpirationDateTime(LocalDateTime.now().minusMinutes(1));

//         when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(entity));
//         when(personalInfoRepository.save(any(PersonalInfoEntity.class))).thenReturn(entity);

//         // Act & Assert
//         FailedToSendOTPException exception = assertThrows(FailedToSendOTPException.class, () ->
//             emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, TEST_ACCOUNT_ID)
//         );
//         assertEquals("Webank OTP expired", exception.getMessage());
//         verify(personalInfoRepository).save(entity);
//     }

//     @Test
//     void testValidateEmailOtp_InvalidOtp() {
//         // Arrange
//         PersonalInfoEntity entity = new PersonalInfoEntity();
//         entity.setAccountId(TEST_ACCOUNT_ID);
//         entity.setEmailOtpCode(TEST_OTP);
//         entity.setEmailOtpHash(emailOtpService.computeOtpHash(TEST_OTP, TEST_ACCOUNT_ID));
//         entity.setOtpExpirationDateTime(LocalDateTime.now().plusMinutes(5));
        
//         when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(entity));
//         when(personalInfoRepository.save(any(PersonalInfoEntity.class))).thenReturn(entity);

//         // Act & Assert
//         ValidationException exception = assertThrows(ValidationException.class, () ->
//             emailOtpService.validateEmailOtp(TEST_EMAIL, "654321", TEST_ACCOUNT_ID)
//         );
//         assertEquals("Invalid OTP", exception.getMessage());
//         verify(personalInfoRepository).save(entity);
//     }

//     @Test
//     void testValidateEmailOtp_UserNotFound() {
//         // Arrange
//         when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

//         // Act & Assert
//         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
//             emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, TEST_ACCOUNT_ID)
//         );
//         assertEquals("User record not found", exception.getMessage());
//         verify(personalInfoRepository, never()).save(any(PersonalInfoEntity.class));
//     }

//     @Test
//     void testComputeOtpHash() {
//         // Arrange
//         String expectedHash = emailOtpService.computeHash(
//             emailOtpService.canonicalizeJson(
//                 String.format("{\"emailOtp\":\"%s\", \"accountId\":\"%s\", \"salt\":\"%s\"}",
//                     TEST_OTP, TEST_ACCOUNT_ID, TEST_SALT)
//             )
//         );

//         // Act
//         String actualHash = emailOtpService.computeOtpHash(TEST_OTP, TEST_ACCOUNT_ID);

//         // Assert
//         assertEquals(expectedHash, actualHash);
//     }

//     @Test
//     void testCanonicalizeJson() {
//         // Arrange
//         String input = "{\"b\":2,\"a\":1}";
//         String expected = "{\"a\":1,\"b\":2}";

//         // Act
//         String result = emailOtpService.canonicalizeJson(input);

//         // Assert
//         assertEquals(expected, result);
//     }
// } 