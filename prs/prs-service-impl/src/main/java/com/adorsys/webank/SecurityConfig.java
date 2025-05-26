package com.adorsys.webank;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for the application.
 * Provides password encoding with Argon2, the winner of the Password Hashing Competition.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Creates a PasswordEncoder that uses Argon2 for password hashing.
     * Argon2 is configured with optimal parameters for security and performance.
     *
     * @return The configured PasswordEncoder
     */
    /**
     * Configures the security filter chain for the application.
     * Enables CORS and disables CSRF for REST API access.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if there's an error configuring security
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(
                authorize -> authorize.anyRequest().permitAll() // Since we're using JWT-based authentication
            );

        return http.build();
    }

    /**
     * Configures CORS for Spring Security to allow cross-origin requests from the frontend.
     *
     * @return a CorsConfigurationSource bean
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(
            Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        );
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Creates a PasswordEncoder that uses Argon2 for password hashing.
     * Argon2 is configured with optimal parameters for security and performance.
     *
     * @return The configured PasswordEncoder
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        // Create a map of encoders to support legacy encoding during migration
        Map<String, PasswordEncoder> encoders = new HashMap<>();

        // Configure Argon2 parameters:
        // - saltLength: 16 bytes for salt (128 bits)
        // - hashLength: 32 bytes for hash (256 bits)
        // - parallelism: 1 (can be increased on multi-core servers)
        // - memory: 16384 KiB (1 << 14)
        // - iterations: 2 (higher is more secure but slower)
        Argon2PasswordEncoder argon2PasswordEncoder =
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        // Set argon2 as our primary encoder
        encoders.put("argon2", argon2PasswordEncoder);

        // Create a delegating password encoder with argon2 as the default
        return new DelegatingPasswordEncoder("argon2", encoders);
    }
}
