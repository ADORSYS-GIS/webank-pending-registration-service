package com.adorsys.webank.serviceimpl.security;

import com.adorsys.webank.config.CertValidator;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.adorsys.webank.config.KeyLoader;

import static org.junit.jupiter.api.Assertions.*;

class CertValidatorTest {

    private CertValidator certValidator;
    private ECKey serverPrivateKey;
    private KeyLoader keyLoader;

    @BeforeEach
    void setUp() throws JOSEException {
        // Create KeyLoader instance
        keyLoader = new KeyLoader();

        // Create EC key pair
        ECKey ecKey = new ECKeyGenerator(Curve.P_256).generate();
        serverPrivateKey = ecKey;
        String serverPublicKeyJson = ecKey.toPublicJWK().toJSONString();

        // Set public key in KeyLoader
        ReflectionTestUtils.setField(keyLoader, "serverPublicKeyJson", serverPublicKeyJson);

        // Initialize CertValidator with KeyLoader
        certValidator = new CertValidator(keyLoader);
    }
    @Test
    void validateJWT_validToken_returnsTrue() throws JOSEException {
        SignedJWT devJwt = createSignedJWT(serverPrivateKey);
        SignedJWT mainJwt = createMainJWT(devJwt, serverPrivateKey);

        assertTrue(certValidator.validateJWT(mainJwt.serialize()));
    }

    @Test
    void validateJWT_missingDevJwt_returnsFalse() throws JOSEException {
        SignedJWT mainJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256).build(),
                new JWTClaimsSet.Builder().build());
        mainJwt.sign(new ECDSASigner(serverPrivateKey));

        assertFalse(certValidator.validateJWT(mainJwt.serialize()));
    }

    @Test
    void validateJWT_invalidDevJwtSignature_returnsFalse() throws JOSEException {
        ECKey otherKey = new ECKeyGenerator(Curve.P_256).generate();
        SignedJWT devJwt = createSignedJWT(otherKey);
        SignedJWT mainJwt = createMainJWT(devJwt, serverPrivateKey);

        assertFalse(certValidator.validateJWT(mainJwt.serialize()));
    }

    @Test
    void validateJWT_invalidPublicKey_returnsFalse() throws JOSEException {
        ReflectionTestUtils.setField(keyLoader, "serverPublicKeyJson", "invalid_json");
        SignedJWT devJwt = createSignedJWT(serverPrivateKey);
        SignedJWT mainJwt = createMainJWT(devJwt, serverPrivateKey);

        assertFalse(certValidator.validateJWT(mainJwt.serialize()));
    }

    // Helper: creates a simple signed JWT.
    private SignedJWT createSignedJWT(ECKey signingKey) throws JOSEException {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().build();
        SignedJWT jwt = new SignedJWT(header, claimsSet);
        jwt.sign(new ECDSASigner(signingKey));
        return jwt;
    }

    // Helper: creates a main JWT that includes a serialized devJwt in a custom header parameter.
    private SignedJWT createMainJWT(SignedJWT devJwt, ECKey signingKey) throws JOSEException {
        JWSHeader mainHeader = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .customParam("devJwt", devJwt.serialize())
                .build();
        SignedJWT mainJwt = new SignedJWT(mainHeader, new JWTClaimsSet.Builder().build());
        mainJwt.sign(new ECDSASigner(signingKey));
        return mainJwt;
    }
}
