package com.adorsys.webank.config;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Slf4j
@Component
public class KeyLoader {

    @Value("${server.private.key}")
    private String serverPrivateKeyJson;

    @Value("${server.public.key}")
    private String serverPublicKeyJson;

    public ECKey loadPrivateKey() throws ParseException {
        if (serverPrivateKeyJson == null || serverPrivateKeyJson.trim().isEmpty()) {
            log.error("Server private key is null or empty");
            throw new IllegalStateException("Server private key JSON must not be null or empty");
        }

        ECKey key = (ECKey) JWK.parse(serverPrivateKeyJson);
        if (key.getD() == null) {
            log.error("Private key is missing 'd' parameter");
            throw new IllegalStateException("Private key 'd' parameter is missing");
        }
        log.info("Loaded private key from backend: {} ", key.toJSONString());
        return key;
    }

    public ECKey loadPublicKey() throws ParseException {
        if (serverPublicKeyJson == null || serverPublicKeyJson.trim().isEmpty()) {
            log.error("Server public key is null or empty");
            throw new IllegalStateException("Server public key JSON must not be null or empty");
        }
        log.info("Loading public key from backend: {} ", serverPublicKeyJson);
        return (ECKey) JWK.parse(serverPublicKeyJson);
    }
}