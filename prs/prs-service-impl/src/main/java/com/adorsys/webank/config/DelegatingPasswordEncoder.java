package com.adorsys.webank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class for security-related beans.
 */
@Configuration
public class DelegatingPasswordEncoder {

    /**
     * Creates and configures an Argon2PasswordEncoder bean with recommended parameters.
     *
     * @return Configured Argon2PasswordEncoder instance
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        // Parameters: saltLength, hashLength, parallelism, memory, iterations
        return new Argon2PasswordEncoder(16, 32, 1, 4096, 2);
    }
}