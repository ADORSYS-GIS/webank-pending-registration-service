package com.adorsys.webank;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables Spring Security configuration with Argon2 password hashing.
 * Apply this annotation to a Spring Boot application class to import
 * the security configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({
    SecurityConfig.class
})
public @interface EnableSecurity {
}