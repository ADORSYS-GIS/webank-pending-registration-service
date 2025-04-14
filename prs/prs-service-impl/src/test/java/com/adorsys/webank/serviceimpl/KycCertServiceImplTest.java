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
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KycCertServiceImplTest {

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @Mock
    private CertGeneratorHelper certGeneratorHelper;

    @InjectMocks
    private KycCertServiceImpl kycCertService;

    private JWK publicKey;
    private static final String TEST_ACCOUNT_ID = "test-account-id";

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        MockitoAnnotations.openMocks(this);
        publicKey = generateTestPublicKey();
    }

    @Test
    void testGetCert_WhenPersonalInfoExistsAndApproved() throws Exception {
        // Arrange
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setAccountId(TEST_ACCOUNT_ID);
        personalInfo.setStatus(PersonalInfoStatus.APPROVED);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(personalInfo));
        when(certGeneratorHelper.generateCertificate(publicKey.toJSONString())).thenReturn("test-certificate");

        // Act
        String result = kycCertService.getCert(publicKey, TEST_ACCOUNT_ID);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.contains("Your certificate is: test-certificate"), "Certificate should be generated");

        verify(personalInfoRepository, times(1)).findByAccountId(TEST_ACCOUNT_ID);
        verify(certGeneratorHelper, times(1)).generateCertificate(publicKey.toJSONString());
    }

    @Test
    void testGetCert_WhenPersonalInfoDoesNotExist() {
        // Arrange
        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // Act
        String result = kycCertService.getCert(publicKey, TEST_ACCOUNT_ID);

        // Assert
        assertEquals("null", result, "Result should be 'null' when personal info does not exist");

        verify(personalInfoRepository, times(1)).findByAccountId(TEST_ACCOUNT_ID);
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    @Test
    void testGetCert_WhenPersonalInfoExistsButNotApproved() {
        // Arrange
        PersonalInfoEntity personalInfo = new PersonalInfoEntity();
        personalInfo.setAccountId(TEST_ACCOUNT_ID);
        personalInfo.setStatus(PersonalInfoStatus.PENDING);

        when(personalInfoRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(personalInfo));

        // Act
        String result = kycCertService.getCert(publicKey, TEST_ACCOUNT_ID);

        // Assert
        assertEquals("null", result, "Result should be 'null' when personal info is not approved");

        verify(personalInfoRepository, times(1)).findByAccountId(TEST_ACCOUNT_ID);
        verify(certGeneratorHelper, never()).generateCertificate(anyString());
    }

    private JWK generateTestPublicKey() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateECKeyPair();
        return new ECKey.Builder(Curve.P_256, (java.security.interfaces.ECPublicKey) keyPair.getPublic()).build();
    }

    private KeyPair generateECKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256); // Use P-256 curve
        return keyPairGenerator.generateKeyPair();
    }
}