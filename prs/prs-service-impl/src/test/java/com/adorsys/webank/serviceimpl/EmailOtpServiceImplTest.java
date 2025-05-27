package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.Curve;
import jakarta.mail.internet.MimeMessage;
import org.erdtman.jcs.JsonCanonicalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailOtpServiceImplTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailOtpServiceImplTest.class);

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

    @BeforeEach
    public void setUp() throws Exception {
        deviceKey = new ECKeyGenerator(Curve.P_256).generate();

        // Inject mailSender using reflection
        Field mailSenderField = EmailOtpServiceImpl.class.getDeclaredField("mailSender");
        mailSenderField.setAccessible(true);
        mailSenderField.set(emailOtpService, mailSender);

        // Inject salt and fromEmail using reflection
        setField("salt", TEST_SALT);
        setField("fromEmail", "no-reply@test.com");
    }

    private void setField(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = EmailOtpServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(emailOtpService, value);
    }

    // @Test
    // public void testGenerateOtp() {
    //     String otp = emailOtpService.generateOtp();
    //     assertEquals(6, otp.length());
    //     assertTrue(otp.matches("\\d+"));
    // }

    // @Test
    // public void testSendEmailOtp_Success() throws Exception {
    //     // Arrange
    //     String accountId = computePublicKeyHash(deviceKey.toJSONString());
    //     PersonalInfoEntity entity = new PersonalInfoEntity();
    //     entity.setAccountId(accountId);

    //     when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(entity));
    //     when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));

    //     // Act & Assert
    //     assertDoesNotThrow(() -> {
    //         String result = emailOtpService.sendEmailOtp(accountId, TEST_EMAIL);
    //         assertEquals("OTP sent successfully to " + TEST_EMAIL, result);
    //     });

    //     verify(mailSender, times(1)).send(any(MimeMessage.class));
    // }

    // @Test
    // public void testSendEmailOtp_InvalidEmail() {
    //     String accountId = computePublicKeyHash(deviceKey.toJSONString());
    //     assertThrows(IllegalArgumentException.class, () ->
    //             emailOtpService.sendEmailOtp(accountId, "invalid-email")
    //     );
    // }

    // @Test
    // public void testValidateEmailOtp_Valid() throws Exception {
    //     // Arrange
    //     String accountId = computePublicKeyHash(deviceKey.toJSONString());
    //     PersonalInfoEntity entity = new PersonalInfoEntity();
    //     entity.setAccountId(accountId);
    //     entity.setEmailOtpCode(TEST_OTP);
    //     entity.setEmailOtpHash(computeOtpHash(TEST_OTP, accountId));
    //     entity.setOtpExpirationDateTime(LocalDateTime.now().plusMinutes(1));

    //     when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(entity));

    //     // Act
    //     String result = emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, accountId);

    //     // Assert
    //     assertEquals("Webank email verified successfully", result);
    //     assertEquals(TEST_EMAIL, entity.getEmail());
    //     verify(personalInfoRepository).save(entity);
    // }

    // @Test
    // public void testValidateEmailOtp_Expired() {
    //     String accountId = computePublicKeyHash(deviceKey.toJSONString());
    //     PersonalInfoEntity entity = new PersonalInfoEntity();
    //     entity.setAccountId(accountId);
    //     entity.setOtpExpirationDateTime(LocalDateTime.now().minusMinutes(1));

    //     when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(entity));

    //     assertThrows(IllegalArgumentException.class, () ->
    //             emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, accountId)
    //     );
    // }

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