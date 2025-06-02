package com.adorsys.webank.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.validation.constraints.NotBlank;

@Data
@Component
@ConfigurationProperties(prefix = "server")
public class ServerKeyProperties {
    /**
     * Server private key in JWK format (usually injected from environment variable SERVER_PRIVATE_KEY_JSON)
     */
    @NotBlank(message = "Private key must not be blank")
    private String privateKey;

    /**
     * Server public key in JWK format (usually injected from environment variable SERVER_PUBLIC_KEY_JSON)
     */
    @NotBlank(message = "Public key must not be blank")
    private String publicKey;
}
