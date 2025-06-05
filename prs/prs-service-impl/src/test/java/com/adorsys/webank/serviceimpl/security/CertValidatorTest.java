package com.adorsys.webank.serviceimpl.security;

import com.adorsys.webank.config.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class CertValidatorTest {

    private CertValidator certValidator;
    private ECKey serverPrivateKey;

    @BeforeEach
    void setUp() throws JOSEException {
        // Create an EC key pair
        ECKey ecKey = new ECKeyGenerator(Curve.P_256).generate();
        serverPrivateKey = ecKey;
        String serverPublicKeyJson = ecKey.toPublicJWK().toJSONString();

        // Create and configure ServerKeyProperties
        ServerKeyProperties keyProperties = new ServerKeyProperties();
        keyProperties.setPublicKey(serverPublicKeyJson);
        // You can also setPrivateKey if needed

        // Inject into KeyLoader
        KeyLoader keyLoader = new KeyLoader(keyProperties);

        // Initialize CertValidator
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
        ServerKeyProperties invalidProps = new ServerKeyProperties();
        invalidProps.setPublicKey("invalid_json");

        KeyLoader invalidLoader = new KeyLoader(invalidProps);
        CertValidator invalidCertValidator = new CertValidator(invalidLoader);

        SignedJWT devJwt = createSignedJWT(serverPrivateKey);
        SignedJWT mainJwt = createMainJWT(devJwt, serverPrivateKey);

        assertFalse(invalidCertValidator.validateJWT(mainJwt.serialize()));
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
