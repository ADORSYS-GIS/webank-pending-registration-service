package com.adorsys.webank.serviceimpl.security;

import com.adorsys.webank.security.JwtValidator;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidatorTest {

    private ECKey validEcKey;
    private ECKey invalidEcKey;

    @BeforeEach
    void setUp() throws JOSEException {
        validEcKey = new ECKeyGenerator(Curve.P_256).generate();
        invalidEcKey = new ECKeyGenerator(Curve.P_256).generate();
    }

    @Test
    void validateAndExtract_validJwt_returnsJwk()
            throws JOSEException, ParseException, BadJOSEException, NoSuchAlgorithmException, com.fasterxml.jackson.core.JsonProcessingException {
        String[] params = {"param1", "param2"};
        String concatenated = String.join("", params);
        String hash = JwtValidator.hashPayload(concatenated);

        SignedJWT signedJWT = createSignedJWT(validEcKey, hash);
        JWK jwk = JwtValidator.validateAndExtract(signedJWT.serialize(), params);
        assertEquals(validEcKey.toPublicJWK(), jwk);
    }

    @Test
    void validateAndExtract_missingJwk_throwsBadJOSEException() throws JOSEException {
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256).build(),
                new JWTClaimsSet.Builder().build());
        signedJWT.sign(new ECDSASigner(validEcKey));

        assertThrows(BadJOSEException.class, () ->
                JwtValidator.validateAndExtract(signedJWT.serialize(), "param"));
    }

    @Test
    void validateAndExtract_invalidKeyType_throwsBadJOSEException() throws JOSEException {
        RSAKey rsaKey = new RSAKeyGenerator(2048).generate();
        SignedJWT signedJWT = createSignedJWTWithRSA(rsaKey, "dummyHash");

        assertThrows(BadJOSEException.class, () ->
                JwtValidator.validateAndExtract(signedJWT.serialize(), "dummy"));
    }

    @Test
    void validateAndExtract_invalidSignature_throwsBadJWTException()
            throws JOSEException, ParseException, BadJOSEException, NoSuchAlgorithmException, com.fasterxml.jackson.core.JsonProcessingException {
        // Instead of tampering with the token, create a JWT where the header contains validEcKey's public key
        // but sign the token with invalidEcKey.
        String hash = JwtValidator.hashPayload("originalPayload");
        SignedJWT invalidJWT = createSignedJWTWithMismatchedKey(invalidEcKey, validEcKey, hash);

        assertThrows(BadJWTException.class, () ->
                JwtValidator.validateAndExtract(invalidJWT.serialize(), "originalPayload"));
    }

    @Test
    void validateAndExtract_hashMismatch_throwsBadJWTException()
            throws JOSEException, ParseException, BadJOSEException, NoSuchAlgorithmException, com.fasterxml.jackson.core.JsonProcessingException {
        // Use a wrong hash in the JWT claims.
        SignedJWT signedJWT = createSignedJWT(validEcKey, "wrongHash");

        assertThrows(BadJWTException.class, () ->
                JwtValidator.validateAndExtract(signedJWT.serialize(), "correct"));
    }

    @Test
    void hashPayload_validInput_returnsCorrectHash() throws NoSuchAlgorithmException {
        String hash = JwtValidator.hashPayload("test");
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", hash);
    }

    // Helper: creates a JWT signed with the provided key and embeds the public key in the header.
    private SignedJWT createSignedJWT(ECKey key, String hash) throws JOSEException {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .jwk(key.toPublicJWK())
                .build();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("hash", hash)
                .build();
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(new ECDSASigner(key));
        return signedJWT;
    }

    // Helper: creates a JWT signed with an RSA key.
    private SignedJWT createSignedJWTWithRSA(RSAKey key, String hash) throws JOSEException {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .jwk(key.toPublicJWK())
                .build();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("hash", hash)
                .build();
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(new RSASSASigner(key));
        return signedJWT;
    }

    // Helper: creates a JWT where the header contains headerKey’s public part but the token is signed with signingKey.
    private SignedJWT createSignedJWTWithMismatchedKey(ECKey signingKey, ECKey headerKey, String hash) throws JOSEException {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .jwk(headerKey.toPublicJWK())
                .build();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("hash", hash)
                .build();
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(new ECDSASigner(signingKey));
        return signedJWT;
    }
}
