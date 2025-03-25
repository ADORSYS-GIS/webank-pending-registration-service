package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.repository.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;
import jakarta.mail.internet.*;
import org.erdtman.jcs.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.mail.javamail.*;

import java.lang.reflect.*;
import java.security.*;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailOtpServiceImplTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailOtpServiceImplTest.class);


    @Mock
    private  PersonalInfoRepository personalInfoRepository;

    @Mock
    private JavaMailSender mailSender;


    @InjectMocks
    private EmailOtpServiceImpl emailOtpService;

    private ECKey deviceKey;
    private  static final String TEST_EMAIL = "user@example.com";
    private  static final String TEST_OTP = "123456";
    private  static final String TEST_SALT = "test-salt";

    @BeforeEach
    public void setUp() throws Exception {
        deviceKey = new ECKeyGenerator(Curve.P_256).generate();

        // Fix: Manually inject mocks that use @Resource
        Field mailSenderField = EmailOtpServiceImpl.class.getDeclaredField("mailSender");
        mailSenderField.setAccessible(true);
        mailSenderField.set(emailOtpService, mailSender);


        // Inject mock values using reflection
        setField("salt", TEST_SALT);
        setField("fromEmail", "no-reply@test.com");
    }

    // Fixed method: Declare specific exceptions instead of generic Exception
    private void setField(String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
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
        String devicePublicKeyJson = deviceKey.toJSONString(); // Actual key
        PersonalInfoEntity entity = new PersonalInfoEntity();
        entity.setPublicKeyHash(computePublicKeyHash(devicePublicKeyJson)); // Use real key

        when(personalInfoRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(entity));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));

        // Act & Assert
        assertDoesNotThrow(() -> {
            String result = emailOtpService.sendEmailOtp(deviceKey, TEST_EMAIL);
            assertEquals("OTP sent successfully to " + TEST_EMAIL, result);
        });

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    public void testSendEmailOtp_InvalidEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                emailOtpService.sendEmailOtp(deviceKey, "invalid-email")
        );
    }
    @Test
    public void testValidateEmailOtp_Valid() throws Exception {
        // Arrange
        String devicePublicKeyJson = deviceKey.toJSONString(); // Get actual key JSON
        PersonalInfoEntity entity = createTestEntity();
        entity.setEmailOtpHash(computeOtpHash(TEST_OTP, devicePublicKeyJson)); // Use real key
        entity.setOtpExpirationDateTime(LocalDateTime.now().plusMinutes(1));

        when(personalInfoRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(entity));

        // Act
        String result = emailOtpService.validateEmailOtp(TEST_EMAIL, deviceKey, TEST_OTP);

        // Assert
        assertEquals("Webank email verified successfully", result);
        assertEquals(TEST_EMAIL, entity.getEmail());
        verify(personalInfoRepository).save(entity);
    }

    @Test
    public void testValidateEmailOtp_Expired() {
        PersonalInfoEntity entity = createTestEntity();
        entity.setOtpExpirationDateTime(LocalDateTime.now().minusMinutes(1));

        when(personalInfoRepository.findByPublicKeyHash(any())).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () ->
                emailOtpService.validateEmailOtp(TEST_EMAIL, deviceKey, TEST_OTP)
        );
    }

    @Test
    public void testComputeOtpHash() throws Exception {
        String devicePublicKeyJson = deviceKey.toJSONString();
        String inputJson = String.format("{\"emailOtp\":\"%s\", \"devicePub\":%s, \"salt\":\"%s\"}",
                TEST_OTP, devicePublicKeyJson, TEST_SALT);

        String canonicalJson = new JsonCanonicalizer(inputJson).getEncodedString();
        String expectedHash = bytesToHex(MessageDigest.getInstance("SHA-256")
                .digest(canonicalJson.getBytes()));

        String actualHash = emailOtpService.computeOtpHash(TEST_OTP, devicePublicKeyJson);

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

    private PersonalInfoEntity createTestEntity() {
        String devicePublicKeyJson = deviceKey.toJSONString();
        PersonalInfoEntity entity = new PersonalInfoEntity();
        entity.setPublicKeyHash(computePublicKeyHash(devicePublicKeyJson));
        return entity;
    }

    private String computePublicKeyHash(String publicKeyJson) {
        return emailOtpService.computeHash(publicKeyJson);
    }

    private String computeOtpHash(String otp, String publicKeyJson) {
        return emailOtpService.computeOtpHash(otp, publicKeyJson);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}