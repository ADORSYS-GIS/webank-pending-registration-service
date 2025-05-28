package com.adorsys.webank.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for the Webank application.
 * Configures CORS at the Spring MVC level, complementing the CORS settings in WebSecurityConfig.
 */
@Configuration
@Slf4j
public class WebConfig {
    /**
     * Configures CORS settings at the Spring MVC level.
     * Note: This works in conjunction with the CORS settings in WebSecurityConfig.
     * 
     * @return A WebMvcConfigurer with CORS mappings
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}