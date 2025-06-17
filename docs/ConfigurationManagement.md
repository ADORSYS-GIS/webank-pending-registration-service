# Configuration Management

This document outlines the configuration management approach for the `webank-pending-registration-service` application. The goal is to provide a robust, secure, and maintainable way to manage application settings.

## 1. Overview

We use Spring Boot's externalized configuration mechanism, with a focus on type-safe properties and validation. This approach helps prevent configuration errors and makes the application more resilient.

Key features of our configuration strategy include:
- **Type-Safe Configuration Properties**: We use `@ConfigurationProperties` to bind configuration from `application.yml` to strongly-typed Java objects.
- **Validation**: Configuration properties are validated at startup using Java's validation API (`javax.validation.constraints`). This ensures that the application fails fast if the configuration is invalid.
- **Environment-Specific Configuration**: We use environment variables to inject sensitive data and environment-specific settings, keeping secrets out of the codebase.
- **IDE Support**: The `spring-boot-configuration-processor` dependency is included to generate metadata for configuration properties, providing autocompletion and documentation in IDEs like IntelliJ IDEA and VS Code.

## 2. Configuration Files

The primary configuration file is `prs/prs-rest-server/src/main/resources/application.yml`. This file contains the default configuration for the application.

### 2.1. Environment Variables

Sensitive information and environment-specific values are injected using environment variables. This is a security best practice that avoids hardcoding secrets in the source code. The following environment variables are required:

- `EMAIL`: The username for the SMTP server.
- `PASSWORD`: The password for the SMTP server.
- `OTP_SALT`: A secret salt used for hashing One-Time Passwords (OTPs).
- `SERVER_PRIVATE_KEY_JSON`: The server's private key in JWK (JSON Web Key) format.
- `SERVER_PUBLIC_KEY_JSON`: The server's public key in JWK (JSON Web Key) format.
- `JWT_ISSUER`: The issuer for JSON Web Tokens (JWTs).
- `JWT_EXPIRATION_TIME_MS`: The expiration time for JWTs in milliseconds.

These variables are referenced in `application.yml` using the `${...}` syntax.

## 3. Type-Safe Configuration Properties

We have created several `@ConfigurationProperties` classes to manage different parts of the application's configuration. These classes are located in the `prs/prs-middleware/src/main/java/com/adorsys/webank/properties/` package.

Each class is annotated with `@ConfigurationProperties(prefix = "...")`, which tells Spring Boot to bind properties starting with that prefix to the fields of the class. They are also annotated with `@Validated` to trigger validation.

### 3.1. List of Configuration Properties Classes

- **`ApplicationProperties`**: Binds `spring.application.name`.
- **`JwtProperties`**: Binds JWT-related properties under the `jwt` prefix.
- **`MailProperties`**: Binds mail-related properties under the `spring.mail` prefix.
- **`OtpProperties`**: Binds OTP-related properties under the `otp` prefix.
- **`ServerKeysProperties`**: Binds server key properties under the `server` prefix.
- **`SpringDocProperties`**: Binds SpringDoc/OpenAPI properties under the `springdoc` prefix.

### 3.2. Example: `JwtProperties`

```java
package com.adorsys.webank.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank(message = "JWT issuer is required")
    private String issuer;

    @Positive(message = "JWT expiration time must be positive")
    private Long expirationTimeMs;
}
```

## 4. Validation

Validation annotations (e.g., `@NotBlank`, `@Positive`, `@Email`) are used on the fields of the configuration properties classes. If any of these constraints are violated at application startup, the application will fail to start with an informative error message. This prevents runtime errors caused by misconfiguration.

## 5. How to Use in Code

To use the configuration properties, simply inject the corresponding properties class into your Spring components using constructor injection.

### Example: Using `JwtProperties` in a Service

```java
import com.adorsys.webank.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyService {

    private final JwtProperties jwtProperties;

    public void someMethod() {
        String issuer = jwtProperties.getIssuer();
        Long expiration = jwtProperties.getExpirationTimeMs();
        // ... use the properties
    }
}
```

This approach makes the code cleaner, safer, and more maintainable compared to using `@Value` annotations everywhere.
