package com.adorsys.webank.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import org.erdtman.jcs.JsonCanonicalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;

import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmailOtpServiceImplTest {
    private static final Logger log = LoggerFactory.getLogger(EmailOtpServiceImplTest.class);

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailOtpServiceImpl emailOtpService;

    private ECKey deviceKey;
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_OTP = "123456";
    private static final String TEST_SALT = "test-salt";
    private static final String TEST_CORRELATION_ID = "test-correlation-id";
    private static final String TEST_ACCOUNT_ID = "test-account-id";

    @BeforeEach
    public void setUp() throws Exception {
        deviceKey = new ECKeyGenerator(Curve.P_256).generate();

        // Set correlation ID for testing
        MDC.put("correlationId", TEST_CORRELATION_ID);

        // Inject mailSender using reflection
        Field mailSenderField = EmailOtpServiceImpl.class.getDeclaredField("mailSender");
        mailSenderField.setAccessible(true);
        mailSenderField.set(emailOtpService, mailSender);

        // Inject salt and fromEmail using reflection
        ReflectionTestUtils.setField(emailOtpService, "salt", TEST_SALT);
        ReflectionTestUtils.setField(emailOtpService, "fromEmail", "no-reply@test.com");
    }

    @Test
    public void testGenerateOtp() {
        String otp = emailOtpService.generateOtp();
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d+"));
    }

    @Test
    public void testSendEmailOtp_Success() throws Exception {
        // Arrange
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        PersonalInfoProjection projection = mock(PersonalInfoProjection.class);
        when(projection.getAccountId()).thenReturn(accountId);

        when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(projection));
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act & Assert
        assertDoesNotThrow(() -> {
            String result = emailOtpService.sendEmailOtp(accountId, TEST_EMAIL);
            assertEquals("Email OTP sent successfully", result);
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
        PersonalInfoProjection projection = mock(PersonalInfoProjection.class);
        when(projection.getAccountId()).thenReturn(accountId);
        when(projection.getEmailOtpCode()).thenReturn(TEST_OTP);
        when(projection.getEmailOtpHash()).thenReturn(computeOtpHash(TEST_OTP, accountId));
        when(projection.getOtpExpirationDateTime()).thenReturn(LocalDateTime.now().plusMinutes(1));

        when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(projection));

        // Act
        String result = emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, accountId);

        // Assert
        assertEquals("Email OTP validated successfully", result);
        verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
    }

    @Test
    public void testValidateEmailOtp_Expired() {
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        PersonalInfoProjection projection = mock(PersonalInfoProjection.class);
        when(projection.getAccountId()).thenReturn(accountId);
        when(projection.getOtpExpirationDateTime()).thenReturn(LocalDateTime.now().minusMinutes(1));

        when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(projection));

        assertThrows(FailedToSendOTPException.class, () ->
                emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, accountId)
        );
    }

    @Test
    public void testComputeOtpHash() throws Exception {
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        String inputJson = String.format("{\"emailOtp\":\"%s\", \"accountId\":\"%s\", \"salt\":\"%s\"}",
                TEST_OTP, accountId, TEST_SALT);

        String canonicalJson = new JsonCanonicalizer(inputJson).getEncodedString();
        String expectedHash = bytesToHex(MessageDigest.getInstance("SHA-256")
                .digest(canonicalJson.getBytes()));

        String actualHash = emailOtpService.computeOtpHash(TEST_OTP, accountId);

        log.info("Expected: " + expectedHash);
        log.info("Actual:   " + actualHash);

        assertEquals(expectedHash, actualHash);
    }

    @Test
    public void testCanonicalizeJson() {
        String json = "{\"b\":2, \"a\":1}";
        String canonical = emailOtpService.canonicalizeJson(json);
        assertEquals("{\"a\":1,\"b\":2}", canonical);
    }

    private String computePublicKeyHash(String publicKeyJson) {
        return emailOtpService.computeHash(publicKeyJson);
    }

    private String computeOtpHash(String otp, String accountId) {
        return emailOtpService.computeOtpHash(otp, accountId);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}