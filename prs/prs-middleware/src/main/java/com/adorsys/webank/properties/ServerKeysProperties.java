package com.adorsys.webank.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "server")
public class ServerKeysProperties {
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
