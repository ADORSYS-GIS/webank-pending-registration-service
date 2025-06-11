package com.adorsys.webank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for security-related beans.
 */
@Configuration
public class PasswordEncoderConfig {
    /**
     * Creates and configures a DelegatingPasswordEncoder bean with Argon2 as the default and only encoder.
     *
     * @return Configured DelegatingPasswordEncoder instance
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        String encodingId = "argon2";

        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 4096, 2);

        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(encodingId, argon2);

        return new DelegatingPasswordEncoder(encodingId, encoders);
    }
}