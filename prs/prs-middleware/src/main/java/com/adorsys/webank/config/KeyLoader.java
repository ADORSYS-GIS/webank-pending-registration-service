package com.adorsys.webank.config;

import com.adorsys.webank.properties.ServerKeysProperties;
import com.nimbusds.jose.jwk.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.*;
import com.adorsys.webank.exceptions.SecurityConfigurationException;

import java.text.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyLoader {

    private final ServerKeysProperties keyProperties;


    public ECKey loadPrivateKey() throws ParseException {
        String privateKey = keyProperties.getPrivateKey();

        if (privateKey == null || privateKey.trim().isEmpty()) {
            log.error("Server private key is null or empty");
            throw new SecurityConfigurationException("Server private key JSON must not be null or empty", null);
        }

        ECKey key = (ECKey) JWK.parse(privateKey);
        if (key.getD() == null) {
            log.error("Private key is missing 'd' parameter");
            throw new SecurityConfigurationException("Private key 'd' parameter is missing", null);
        }

        log.info("Loaded private key from backend: {}", key.toJSONString());
        return key;
    }

    public ECKey loadPublicKey() throws ParseException {
        String publicKey = keyProperties.getPublicKey();

        if (publicKey == null || publicKey.trim().isEmpty()) {
            log.error("Server public key is null or empty");
            throw new SecurityConfigurationException("Server public key JSON must not be null or empty", null);
        }

        log.info("Loading public key from backend: {}", publicKey);
        return (ECKey) JWK.parse(publicKey);
    }
}
