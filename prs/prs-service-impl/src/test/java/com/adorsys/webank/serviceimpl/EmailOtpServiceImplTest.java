package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.security.HashHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmailOtpServiceImplTest {

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @Mock
    private JavaMailSender mailSender;

    private EmailOtpServiceImpl emailOtpService;
    

    
    @Mock
    private HashHelper hashHelper;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private Argon2PasswordEncoder passwordEncoder;

    private ECKey deviceKey;
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_OTP = "123456";


    @BeforeEach
    public void setUp() throws Exception {
        deviceKey = new ECKeyGenerator(Curve.P_256).generate();
        
        // Create EmailOtpService with mocked dependencies
        emailOtpService = new EmailOtpServiceImpl(personalInfoRepository, hashHelper, objectMapper, passwordEncoder);
        
        // Inject mailSender using reflection
        Field mailSenderField = EmailOtpServiceImpl.class.getDeclaredField("mailSender");
        mailSenderField.setAccessible(true);
        mailSenderField.set(emailOtpService, mailSender);
        
        // Inject hashHelper using reflection
        Field hashHelperField = EmailOtpServiceImpl.class.getDeclaredField("hashHelper");
        hashHelperField.setAccessible(true);
        hashHelperField.set(emailOtpService, hashHelper);
        
        // Inject fromEmail using reflection
        setField("fromEmail", "no-reply@test.com");
        
        // No need to mock passwordEncoder as it's now an internal implementation detail
        
        // Setup default behavior for hashHelper in lenient mode
        lenient().when(hashHelper.calculateSHA256AsHex(anyString())).thenReturn("deterministicHashValue");
        
        // Setup default behavior for ObjectMapper in lenient mode
        try {
            lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set up mock ObjectMapper", e);
        }
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
    public void testSendEmailOtpSuccess() throws Exception {
        // Arrange
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        PersonalInfoProjection projection = mock(PersonalInfoProjection.class);
        when(projection.getAccountId()).thenReturn(accountId);

        when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(projection));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));

        // Act & Assert
        assertDoesNotThrow(() -> {
            String result = emailOtpService.sendEmailOtp(accountId, TEST_EMAIL);
            assertEquals("OTP sent successfully to " + TEST_EMAIL, result);
        });

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    public void testSendEmailOtpInvalidEmail() {
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        assertThrows(IllegalArgumentException.class, () ->
                emailOtpService.sendEmailOtp(accountId, "invalid-email")
        );
    }

    @Test
    public void testValidateEmailOtpValid() throws Exception {
        // Arrange
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        PersonalInfoProjection projection = mock(PersonalInfoProjection.class);
        when(projection.getAccountId()).thenReturn(accountId);
        when(projection.getEmailOtpCode()).thenReturn(TEST_OTP);
        when(projection.getEmailOtpHash()).thenReturn("$argon2id$v=19$m=4096,t=2,p=1$mockSalt$mockHash");
        when(projection.getOtpExpirationDateTime()).thenReturn(LocalDateTime.now().plusMinutes(1));

        when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(projection));
        
        // Mock the password encoder to always return true for matches
        Argon2PasswordEncoder mockEncoder = mock(Argon2PasswordEncoder.class);
        when(mockEncoder.matches(anyString(), anyString())).thenReturn(true);
        
        // Replace the passwordEncoder in the service using reflection
        Field encoderField = EmailOtpServiceImpl.class.getDeclaredField("passwordEncoder");
        encoderField.setAccessible(true);
        encoderField.set(emailOtpService, mockEncoder);
        
        // Act
        String result = emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, accountId);

        // Assert
        assertEquals("Webank email verified successfully", result);
        verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
    }

    @Test
    public void testValidateEmailOtpExpired() {
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        PersonalInfoProjection projection = mock(PersonalInfoProjection.class);
        when(projection.getAccountId()).thenReturn(accountId);
        when(projection.getOtpExpirationDateTime()).thenReturn(LocalDateTime.now().minusMinutes(1));

        when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(projection));

        assertThrows(IllegalArgumentException.class, () ->
                emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, accountId)
        );
    }

    @Test
    public void testComputeOtpHash() throws Exception {
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        
        // Setup test data
        Map<String, String> testData = new HashMap<>();
        testData.put("emailOtp", TEST_OTP);
        testData.put("accountId", accountId);
        String testJson = "{\"emailOtp\":\"" + TEST_OTP + "\",\"accountId\":\"" + accountId + "\"}";
        String expectedHash = "test_hash_value";
        
        // Mock dependencies
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(testJson);
        when(passwordEncoder.encode(anyString())).thenReturn(expectedHash);
        
        // Call the method
        String actualHash = emailOtpService.computeOtpHash(TEST_OTP, accountId);
        
        // Verify the result
        assertNotNull(actualHash, "Hash should not be null");
        assertEquals(expectedHash, actualHash, "Hash should match the expected value");
        
        // Verify interactions
        verify(objectMapper).writeValueAsString(any(Map.class));
        verify(passwordEncoder).encode(anyString());
    }

    @Test
    public void testCanonicalizeJson() {
        String json = "{\"b\":2, \"a\":1}";
        String canonical = emailOtpService.canonicalizeJson(json);
        assertEquals("{\"a\":1,\"b\":2}", canonical);
    }

    private String computePublicKeyHash(String unused) {
        // Parameter not used in test but kept for method signature clarity
        return "hashValue";  // Return a predictable value for tests
    }

    // The computeOtpHash method has been removed since we now use the actual implementation from the service
}