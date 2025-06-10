package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.model.EmailOtpData;
import com.adorsys.webank.projection.PersonalInfoProjection;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import jakarta.mail.internet.MimeMessage;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmailOtpServiceImplTest {
    private static final Logger log = LoggerFactory.getLogger(EmailOtpServiceImplTest.class);

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmailOtpServiceImpl emailOtpService;

    private ECKey deviceKey;
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_OTP = "123456";
    private static final String TEST_CORRELATION_ID = "test-correlation-id";

    @BeforeEach
    public void setUp() throws Exception {
        deviceKey = new ECKeyGenerator(Curve.P_256).generate();

        // Set correlation ID for testing
        MDC.put("correlationId", TEST_CORRELATION_ID);

        // Initialize the service with required dependencies
        emailOtpService = new EmailOtpServiceImpl(personalInfoRepository, objectMapper, passwordEncoder);
        
        // Set up mail sender
        Field mailSenderField = EmailOtpServiceImpl.class.getDeclaredField("mailSender");
        mailSenderField.setAccessible(true);
        mailSenderField.set(emailOtpService, mailSender);
        
        // Set from email
        Field fromEmailField = EmailOtpServiceImpl.class.getDeclaredField("fromEmail");
        fromEmailField.setAccessible(true);
        fromEmailField.set(emailOtpService, "no-reply@test.com");
        
        // Set up password encoder mock
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded_" + invocation.getArgument(0));
    }

    @SuppressWarnings("unused")
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
        PersonalInfoProjection projection = mock(PersonalInfoProjection.class);
        when(projection.getAccountId()).thenReturn(accountId);

        when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(projection));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"emailOtp\":\"123456\",\"accountId\":\"test\"}");

        // Act
        String result = emailOtpService.sendEmailOtp(accountId, TEST_EMAIL);

        // Assert
        assertEquals("OTP sent successfully to " + TEST_EMAIL, result);
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
        
        // Create a mock projection that will be returned by the repository
        PersonalInfoProjection projection = mock(PersonalInfoProjection.class);
        when(projection.getAccountId()).thenReturn(accountId);
        when(projection.getEmailOtpCode()).thenReturn(TEST_OTP);
        when(projection.getOtpExpirationDateTime()).thenReturn(LocalDateTime.now().plusMinutes(1));
        
        // Compute the expected hash for the OTP
        String expectedHash = "hashed_otp_value";
        when(projection.getEmailOtpHash()).thenReturn(expectedHash);
        
        // Mock the repository to return our mock projection
        when(personalInfoRepository.findByAccountId(accountId)).thenReturn(Optional.of(projection));
        
        // Mock the password encoder to verify the hash
        when(passwordEncoder.matches(anyString(), eq(expectedHash))).thenReturn(true);
        
        // Mock the object mapper for JSON serialization
        when(objectMapper.writeValueAsString(any(EmailOtpData.class)))
            .thenReturn("{\"emailOtp\":\"123456\",\"accountId\":\"" + accountId + "\"}");
        
        // Mock the password encoder for hash computation
        when(passwordEncoder.encode(anyString())).thenReturn(expectedHash);
        
        // Mock the save operation
        PersonalInfoEntity savedEntity = new PersonalInfoEntity();
        when(personalInfoRepository.save(any(PersonalInfoEntity.class))).thenReturn(savedEntity);
        
        // Set up MDC for correlation ID
        MDC.put("correlationId", TEST_CORRELATION_ID);
        
        try {
            // Act
            String result = emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, accountId);
            
            // Assert
            assertEquals("Webank email verified successfully", result);
            
            // Verify interactions
            verify(personalInfoRepository).findByAccountId(accountId);
            verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
            verify(passwordEncoder).matches(anyString(), eq(expectedHash));
            verify(objectMapper).writeValueAsString(any(EmailOtpData.class));
        } finally {
            MDC.clear();
        }
    }

    @Test
    public void testValidateEmailOtp_Expired() {
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
        // Arrange
        String accountId = computePublicKeyHash(deviceKey.toJSONString());
        String expectedHash = "encoded_hashed_value";
        
        // Mock the JSON serialization
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"emailOtp\":\"123456\",\"accountId\":\"test\"}");
        
        // Mock the password encoder
        when(passwordEncoder.encode(anyString())).thenReturn(expectedHash);

        // Act
        String actualHash = emailOtpService.computeOtpHash(TEST_OTP, accountId);

        // Assert
        assertNotNull(actualHash);
        assertEquals(expectedHash, actualHash);
        verify(objectMapper).writeValueAsString(any());
        verify(passwordEncoder).encode(anyString());
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

    @SuppressWarnings("unused")
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}