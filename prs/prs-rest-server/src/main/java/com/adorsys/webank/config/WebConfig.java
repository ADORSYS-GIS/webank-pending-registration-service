package com.adorsys.webank.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc configuration for the application.
 * 
 * Note: This CORS configuration works alongside Spring Security's CORS configuration.
 * While Spring Security's CORS handling takes precedence for secured endpoints,
 * this configuration is still useful for endpoints not protected by security filters.
 */
@Configuration
public class WebConfig {
    /**
     * Configures CORS at the Spring MVC level.
     * 
     * This configuration allows cross-origin requests from the frontend application.
     * It works in conjunction with the CORS configuration in Spring Security.
     * 
     * @return a WebMvcConfigurer that adds CORS mappings
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