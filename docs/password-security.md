# Password Security Implementation - Argon2 Hashing

## Overview

This document describes the implementation of Argon2 password hashing in the Webank Pending Registration Service. Argon2 is the winner of the Password Hashing Competition (PHC) and provides better security than older algorithms like bcrypt, PBKDF2, and scrypt.

This implementation also includes proper CORS (Cross-Origin Resource Sharing) configuration to ensure secure API access from frontend applications.

## Technical Implementation

### 1. Dependencies

The implementation relies on the following dependencies:

```xml
<!-- Spring Security (includes Argon2PasswordEncoder) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Bouncy Castle provider (for the Argon2 implementation) -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.77</version>
</dependency>
```

### 2. Configuration

The `SecurityConfig` class configures the Argon2 password encoder with appropriate parameters:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    
    // Using Spring Security's recommended settings
    Argon2PasswordEncoder argon2PasswordEncoder = 
        Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        
    encoders.put("argon2", argon2PasswordEncoder);
    
    return new DelegatingPasswordEncoder("argon2", encoders);
}
```

### 3. Argon2 Parameters

The Argon2 algorithm is configured with the following parameters:

| Parameter   | Value       | Description                                  |
|-------------|-------------|----------------------------------------------|
| Salt Length | 16 bytes    | Random salt size (128 bits)                  |
| Hash Length | 32 bytes    | Output hash size (256 bits)                  |
| Parallelism | 1           | Number of threads/lanes                      |
| Memory      | 16384 KiB   | Memory cost (1 << 14)                        |
| Iterations  | 2           | Number of iterations (time cost)             |

These parameters were chosen to balance security and performance. The memory cost ensures that password cracking with specialized hardware (like GPUs) is more difficult due to memory constraints.

## Migration Strategy

For existing password hashes, we implemented a seamless migration strategy:

1. The `DelegatingPasswordEncoder` allows identifying and validating passwords hashed with previous algorithms.
2. When a user successfully authenticates with an old hash, their password is automatically rehashed using Argon2.
3. The `passwordMigrationNeeded` flag in the User entity identifies accounts that still need migration.

## Usage Examples

### Encoding a Password

```java
@Autowired
private PasswordEncoder passwordEncoder;

public String encodePassword(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
}
```

### Validating a Password

```java
public boolean validatePassword(String rawPassword, String encodedPassword) {
    return passwordEncoder.matches(rawPassword, encodedPassword);
}
```

## Security Considerations

1. **Hardware Evolution**: Argon2 parameters should be periodically reviewed and adjusted as hardware capabilities evolve.

2. **Memory Requirements**: The memory parameter (16 MiB) affects server memory usage during authentication. Consider this when scaling the application.

3. **Performance Impact**: Password hashing with Argon2 is intentionally slow to deter brute-force attacks. This has minimal impact on legitimate authentication but significantly impedes attackers.

4. **Algorithm Variants**: The implementation uses Argon2id, which combines the security benefits of Argon2i (resistance against side-channel attacks) and Argon2d (resistance against GPU cracking).

5. **CORS Security**: The application implements CORS (Cross-Origin Resource Sharing) at both the Spring MVC level and Spring Security level to ensure secure cross-origin communication. Only authorized frontend origins are allowed to access the API endpoints.

## CORS Configuration

The application uses a dual-layer CORS configuration:

1. **Spring Security CORS**: Configured in `SecurityConfig.java`, this controls CORS at the security filter level, allowing requests from the frontend origin (http://localhost:5173) and handling preflight requests properly.

2. **Spring MVC CORS**: Configured in `WebConfig.java`, this is kept for backward compatibility but is largely superseded by the Spring Security CORS configuration.

This dual-layer approach ensures that both the security filters and MVC handlers properly respect CORS rules, preventing common issues like missing Access-Control-Allow-Origin headers.

## References

1. [Argon2 Paper](https://github.com/P-H-C/phc-winner-argon2/blob/master/argon2-specs.pdf)
2. [Spring Security Documentation - Password Encoding](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
3. [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
4. [Spring Security CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)
5. [OWASP CORS Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)