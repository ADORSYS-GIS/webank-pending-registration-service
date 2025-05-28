package com.adorsys.webank.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Web security configuration for the Webank application.
 * Handles CORS configuration and security settings.
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class WebSecurityConfig {

    /**
     * Configures security settings for the application.
     * - Disables CSRF for token-based authentication
     * - Enables CORS with configured settings
     * - Allows all requests without authentication for now
     * - Disables form login and HTTP Basic authentication
     *
     * @param http The HttpSecurity to modify
     * @return The built SecurityFilterChain
     * @throws Exception If configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Disable CSRF as we're handling token-based authentication
        http.csrf(AbstractHttpConfigurer::disable)
            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Configure authorization for endpoints
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll() // Allow all requests for now
            )
            // Disable form login and HTTP Basic authentication
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Configures CORS settings to allow the frontend to communicate with the API.
     * - Sets allowed origins, methods, and headers
     * - Enables credentials
     * - Sets a max age for preflight requests
     *
     * @return A configured CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:5173")); // Your frontend URL
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With", "Accept",
                "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply to all endpoints
        return source;
    }
}
