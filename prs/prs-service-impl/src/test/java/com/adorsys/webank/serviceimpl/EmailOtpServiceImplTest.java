package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class EmailOtpServiceImplTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailOtpServiceImplTest.class);

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @Mock
    private JavaMailSender mailSender;

    private EmailOtpServiceImpl emailOtpService;
    
    @Mock
    private PasswordHashingService passwordHashingService;

    private ECKey deviceKey;
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_OTP = "123456";


    @BeforeEach
    public void setUp() throws Exception {
        deviceKey = new ECKeyGenerator(Curve.P_256).generate();
        
        // Create EmailOtpService with mocked dependencies
        emailOtpService = new EmailOtpServiceImpl(personalInfoRepository, passwordHashingService);
        
        // Inject mailSender using reflection
        Field mailSenderField = EmailOtpServiceImpl.class.getDeclaredField("mailSender");
        mailSenderField.setAccessible(true);
        mailSenderField.set(emailOtpService, mailSender);
        
        // Inject fromEmail using reflection
        setField("fromEmail", "no-reply@test.com");
        
        // Setup default behavior for passwordHashingService in lenient mode
        lenient().when(passwordHashingService.hash(anyString())).thenReturn("hashedValue");
        lenient().when(passwordHashingService.verify(anyString(), anyString())).thenReturn(true);
    }

    private void setField(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = EmailOtpServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(emailOtpService, value);
    }

    @Test
    public void testGenerateOtp() {
        String otp = emailOtpService.generateOtp();
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d+"));
    }

    @Test
    public void testSendEmailOtp_Success() throws Exception {
        // Arrange
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        PersonalInfoEntity entity = new PersonalInfoEntity();
        entity.setAccountId(accountId);

        when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(entity));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));

        // Act & Assert
        assertDoesNotThrow(() -> {
            String result = emailOtpService.sendEmailOtp(accountId, TEST_EMAIL);
            assertEquals("OTP sent successfully to " + TEST_EMAIL, result);
        });

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    public void testSendEmailOtp_InvalidEmail() {
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        assertThrows(IllegalArgumentException.class, () ->
                emailOtpService.sendEmailOtp(accountId, "invalid-email")
        );
    }

    @Test
    public void testValidateEmailOtp_Valid() throws Exception {
        // Arrange
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        PersonalInfoEntity entity = new PersonalInfoEntity();
        entity.setAccountId(accountId);
        entity.setEmailOtpCode(TEST_OTP);
        entity.setEmailOtpHash(computeOtpHash(TEST_OTP, accountId));
        entity.setOtpExpirationDateTime(LocalDateTime.now().plusMinutes(1));

        when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(entity));

        // Act
        String result = emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, accountId);

        // Assert
        assertEquals("Webank email verified successfully", result);
        assertEquals(TEST_EMAIL, entity.getEmail());
        verify(personalInfoRepository).save(entity);
    }

    @Test
    public void testValidateEmailOtp_Expired() {
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        PersonalInfoEntity entity = new PersonalInfoEntity();
        entity.setAccountId(accountId);
        entity.setOtpExpirationDateTime(LocalDateTime.now().minusMinutes(1));

        when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () ->
                emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, accountId)
        );
    }

    @Test
    public void testComputeOtpHash() throws Exception {
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        String expectedHash = "hashedValue";  // This should match the value returned by our mock
        
        // Configure mock to return the expected value
        when(passwordHashingService.hash(anyString())).thenReturn(expectedHash);
        
        String actualHash = emailOtpService.computeOtpHash(TEST_OTP, accountId);
        
        log.info("Expected: " + expectedHash);
        log.info("Actual:   " + actualHash);
        
        assertEquals(expectedHash, actualHash);
        verify(passwordHashingService).hash(anyString());  // Verify the mock was called
    }

    @Test
    public void testCanonicalizeJson() {
        String json = "{\"b\":2, \"a\":1}";
        String canonical = emailOtpService.canonicalizeJson(json);
        assertEquals("{\"a\":1,\"b\":2}", canonical);
    }

    private String computePublicKeyHash(String publicKeyJson) {
        return "hashValue";  // Return a predictable value for tests
    }

    private String computeOtpHash(String otp, String accountId) {
        return "hashedOtp";  // Return a predictable value for tests
    }


}