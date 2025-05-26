package com.adorsys.webank;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Test configuration for password encoder tests.
 * This configuration provides only the necessary beans for testing password encoding.
 */
@Configuration
public class TestConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        
        // Use the default Spring Security recommended settings for Argon2
        Argon2PasswordEncoder argon2PasswordEncoder = 
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            
        encoders.put("argon2", argon2PasswordEncoder);
        
        return new DelegatingPasswordEncoder("argon2", encoders);
    }
}