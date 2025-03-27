package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.security.CertGeneratorHelper;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KycCertServiceImplTest {

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @InjectMocks
    private KycCertServiceImpl kycCertService;

    private String serverPrivateKeyJson;
    private String serverPublicKeyJson;
    private String issuer = "https://example.com";
    private Long expirationTimeMs = 3600000L; // 1 hour

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        MockitoAnnotations.openMocks(this);

        // Generate an EC key pair for testing
        KeyPair keyPair = generateECKeyPair();
        ECKey ecKey = new ECKey.Builder(Curve.P_256, (java.security.interfaces.ECPublicKey) keyPair.getPublic())
                .privateKey((java.security.interfaces.ECPrivateKey) keyPair.getPrivate())
                .build();

        serverPrivateKeyJson = ecKey.toJSONString();
        serverPublicKeyJson = ecKey.toPublicJWK().toJSONString();

        // Initialize CertGeneratorHelper with mock values
        kycCertService = new KycCertServiceImpl(personalInfoRepository);
        injectCertGeneratorHelper(kycCertService);
    }

    @Test
    void testGetCert_WhenPersonalInfoExistsAndApproved() throws Exception {
        // Arrange
        JWK publicKey = generateTestPublicKey();
        String publicKeyHash = computeHash(publicKey.toJSONString());

        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setPublicKeyHash(publicKeyHash);
        personalInfo.setStatus(PersonalInfoStatus.APPROVED);

        when(personalInfoRepository.findByPublicKeyHash(publicKeyHash)).thenReturn(Optional.of(personalInfo));

        // Act
        String result = kycCertService.getCert(publicKey);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.contains("Your certificate is:"), "Certificate should be generated");

        verify(personalInfoRepository, times(1)).findByPublicKeyHash(publicKeyHash);
    }

    @Test
    void testGetCert_WhenPersonalInfoDoesNotExist() throws Exception {
        // Arrange
        JWK publicKey = generateTestPublicKey();
        String publicKeyHash = computeHash(publicKey.toJSONString());

        when(personalInfoRepository.findByPublicKeyHash(publicKeyHash)).thenReturn(Optional.empty());

        // Act
        String result = kycCertService.getCert(publicKey);

        // Assert
        assertEquals("null", result, "Result should be 'null' when personal info does not exist");

        verify(personalInfoRepository, times(1)).findByPublicKeyHash(publicKeyHash);
    }

    @Test
    void testGetCert_WhenPersonalInfoExistsButNotApproved() throws Exception {
        // Arrange
        JWK publicKey = generateTestPublicKey();
        String publicKeyHash = computeHash(publicKey.toJSONString());

        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setPublicKeyHash(publicKeyHash);
        personalInfo.setStatus(PersonalInfoStatus.PENDING);

        when(personalInfoRepository.findByPublicKeyHash(publicKeyHash)).thenReturn(Optional.of(personalInfo));

        // Act
        String result = kycCertService.getCert(publicKey);

        // Assert
        assertEquals("null", result, "Result should be 'null' when personal info is not approved");

        verify(personalInfoRepository, times(1)).findByPublicKeyHash(publicKeyHash);
    }

    private void injectCertGeneratorHelper(KycCertServiceImpl service) throws NoSuchAlgorithmException {
        CertGeneratorHelper certGeneratorHelper = new CertGeneratorHelper(
                serverPrivateKeyJson,
                serverPublicKeyJson,
                issuer,
                expirationTimeMs
        );

        try {
            var field = KycCertServiceImpl.class.getDeclaredField("certGeneratorHelper");
            field.setAccessible(true);
            field.set(service, certGeneratorHelper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject CertGeneratorHelper", e);
        }
    }

    private JWK generateTestPublicKey() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateECKeyPair();
        return new ECKey.Builder(Curve.P_256, (java.security.interfaces.ECPublicKey) keyPair.getPublic()).build();
    }

    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error computing hash", e);
        }
    }

    private KeyPair generateECKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256); // Use P-256 curve
        return keyPairGenerator.generateKeyPair();
    }
}