//package com.adorsys.webank.serviceimpl;
//
//import com.adorsys.webank.domain.PersonalInfoEntity;
//import com.adorsys.webank.model.EmailOtpData;
//import com.adorsys.webank.repository.PersonalInfoRepository;
//import com.adorsys.webank.serviceimpl.helper.MailHelper;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.nimbusds.jose.jwk.Curve;
//import com.nimbusds.jose.jwk.ECKey;
//import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.MDC;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//public class EmailOtpServiceImplTest {
//    private static final Logger log = LoggerFactory.getLogger(EmailOtpServiceImplTest.class);
//
//    @Mock
//    private PersonalInfoRepository personalInfoRepository;
//
//    @Mock
//    private ObjectMapper objectMapper;
//
//    @Mock
//    private MailHelper mailHelper;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @InjectMocks
//    private EmailOtpServiceImpl emailOtpService;
//
//    private ECKey deviceKey;
//    private static final String TEST_EMAIL = "user@example.com";
//    private static final String TEST_OTP = "123456";
//    private static final String TEST_CORRELATION_ID = "test-correlation-id";
//
//    @BeforeEach
//    public void setUp() throws Exception {
//        deviceKey = new ECKeyGenerator(Curve.P_256).generate();
//
//        // Set correlation ID for testing
//        MDC.put("correlationId", TEST_CORRELATION_ID);
//
//        // Initialize the service with required dependencies
//        emailOtpService = new EmailOtpServiceImpl(personalInfoRepository, objectMapper, passwordEncoder, mailHelper);
//
//        // Set up password encoder mock
//        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded_" + invocation.getArgument(0));
//    }
//
//    @Test
//    public void testGenerateOtp() {
//        String otp = emailOtpService.generateOtp();
//        assertEquals(6, otp.length());
//        assertTrue(otp.matches("\\d+"));
//    }
//
//    @Test
//    public void testSendEmailOtp_Success() throws Exception {
//        // Arrange
//        String accountId = computePublicKeyHash(deviceKey.toJSONString());
//        PersonalInfoEntity entity = new PersonalInfoEntity();
//        entity.setAccountId(accountId);
//
//        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(entity));
//        when(objectMapper.writeValueAsString(any())).thenReturn("{\"emailOtp\":\"123456\",\"accountId\":\"test\"}");
//
//        // Mock MailSendingHelper to do nothing when sendOtpEmail is called
//        doNothing().when(mailHelper).sendOtpEmail(anyString(), anyString());
//
//        // Act
//        String result = emailOtpService.sendEmailOtp(accountId, TEST_EMAIL);
//
//        // Assert
//        assertEquals("OTP sent successfully to " + TEST_EMAIL, result);
//        verify(mailHelper, times(1)).sendOtpEmail(eq(TEST_EMAIL), anyString());
//    }
//
//    @Test
//    public void testSendEmailOtp_InvalidEmail() {
//        String accountId = computePublicKeyHash(deviceKey.toJSONString());
//        assertThrows(IllegalArgumentException.class, () ->
//                emailOtpService.sendEmailOtp(accountId, "invalid-email")
//        );
//    }
//
//    @Test
//    public void testSendEmailOtp_AccountNotFound() {
//        String accountId = "non-existent-account";
//        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.empty());
//
//        assertThrows(com.adorsys.webank.exceptions.AccountNotFoundException.class, () ->
//                emailOtpService.sendEmailOtp(accountId, TEST_EMAIL)
//        );
//    }
//
//    @Test
//    public void testValidateEmailOtp_Valid() throws Exception {
//        // Arrange
//        String accountId = computePublicKeyHash(deviceKey.toJSONString());
//
//        // Create a mock entity that will be returned by the repository
//        PersonalInfoEntity entity = new PersonalInfoEntity();
//        entity.setAccountId(accountId);
//        entity.setEmailOtpCode(TEST_OTP);
//        entity.setOtpExpirationDateTime(LocalDateTime.now().plusMinutes(1));
//
//        // Compute the expected hash for the OTP
//        String expectedHash = "hashed_otp_value";
//        entity.setEmailOtpHash(expectedHash);
//
//        // Mock the repository to return our mock entity
//        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(entity));
//
//        // Mock the password encoder to verify the hash
//        when(passwordEncoder.matches(anyString(), eq(expectedHash))).thenReturn(true);
//
//        // Mock the object mapper for JSON serialization
//        when(objectMapper.writeValueAsString(any(EmailOtpData.class)))
//            .thenReturn("{\"emailOtp\":\"123456\",\"accountId\":\"" + accountId + "\"}");
//
//        // Mock the password encoder for hash computation
//        when(passwordEncoder.encode(anyString())).thenReturn(expectedHash);
//
//        // Mock the save operation
//        PersonalInfoEntity savedEntity = new PersonalInfoEntity();
//        when(personalInfoRepository.save(any(PersonalInfoEntity.class))).thenReturn(savedEntity);
//
//        // Set up MDC for correlation ID
//        MDC.put("correlationId", TEST_CORRELATION_ID);
//
//        try {
//            // Act
//            String result = emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, accountId);
//
//            // Assert
//            assertEquals("Webank email verified successfully", result);
//
//            // Verify interactions
//            verify(personalInfoRepository).findById(accountId);
//            verify(personalInfoRepository).save(any(PersonalInfoEntity.class));
//            verify(passwordEncoder).matches(anyString(), eq(expectedHash));
//            verify(objectMapper).writeValueAsString(any(EmailOtpData.class));
//        } finally {
//            MDC.clear();
//        }
//    }
//
//    @Test
//    public void testValidateEmailOtp_Expired() {
//        String accountId = computePublicKeyHash(deviceKey.toJSONString());
//        PersonalInfoEntity entity = new PersonalInfoEntity();
//        entity.setAccountId(accountId);
//        entity.setOtpExpirationDateTime(LocalDateTime.now().minusMinutes(1));
//
//        when(personalInfoRepository.findById(accountId)).thenReturn(Optional.of(entity));
//
//        assertThrows(IllegalArgumentException.class, () ->
//                emailOtpService.validateEmailOtp(TEST_EMAIL, TEST_OTP, accountId)
//        );
//    }
//
//    @Test
//    public void testComputeOtpHash() throws Exception {
//        // Arrange
//        String accountId = computePublicKeyHash(deviceKey.toJSONString());
//        String expectedHash = "encoded_hashed_value";
//
//        // Mock the JSON serialization
//        when(objectMapper.writeValueAsString(any())).thenReturn("{\"emailOtp\":\"123456\",\"accountId\":\"test\"}");
//
//        // Mock the password encoder
//        when(passwordEncoder.encode(anyString())).thenReturn(expectedHash);
//
//        // Act
//        String actualHash = emailOtpService.computeOtpHash(TEST_OTP, accountId);
//
//        // Assert
//        assertNotNull(actualHash);
//        assertEquals(expectedHash, actualHash);
//        verify(objectMapper).writeValueAsString(any());
//        verify(passwordEncoder).encode(anyString());
//    }
//
//    @Test
//    public void testCanonicalizeJson() {
//        String json = "{\"b\":2, \"a\":1}";
//        String canonical = emailOtpService.canonicalizeJson(json);
//        assertEquals("{\"a\":1,\"b\":2}", canonical);
//    }
//
//    private String computePublicKeyHash(String publicKeyJson) {
//        return emailOtpService.computeHash(publicKeyJson);
//    }
//}