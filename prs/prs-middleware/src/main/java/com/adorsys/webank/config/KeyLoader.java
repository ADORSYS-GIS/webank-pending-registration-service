
package com.adorsys.webank.config;

import com.nimbusds.jose.jwk.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.*;

import java.text.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyLoader {

    private final ServerKeyProperties keyProperties;


    public ECKey loadPrivateKey() throws ParseException {
        String privateKey = keyProperties.getPrivateKey();

        if (privateKey == null || privateKey.trim().isEmpty()) {
            log.error("Server private key is null or empty");
            throw new IllegalStateException("Server private key JSON must not be null or empty");
        }

        ECKey key = (ECKey) JWK.parse(privateKey);
        if (key.getD() == null) {
            log.error("Private key is missing 'd' parameter");
            throw new IllegalStateException("Private key 'd' parameter is missing");
        }

        log.info("Loaded private key from backend: {}", key.toJSONString());
        return key;
    }

    public ECKey loadPublicKey() throws ParseException {
        String publicKey = keyProperties.getPublicKey();

        if (publicKey == null || publicKey.trim().isEmpty()) {
            log.error("Server public key is null or empty");
            throw new IllegalStateException("Server public key JSON must not be null or empty");
        }

        log.info("Loading public key from backend: {}", publicKey);
        return (ECKey) JWK.parse(publicKey);
    }
}
