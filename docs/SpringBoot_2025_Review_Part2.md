## 2. Deprecated Dependencies & Modern Alternatives

### 2.1. Nimbus JOSE JWT Library

**Issue**: The application uses the Nimbus JOSE JWT library for JWT handling, which, while functional, is not the recommended approach for modern Spring Boot applications.

```java
// Current implementation in JwtValidator.java
public static JWK validateAndExtract(String jwtToken, String... params)
        throws ParseException, JOSEException, BadJOSEException, NoSuchAlgorithmException, JsonProcessingException {
    // Complex JWT validation logic
    JWSObject jwsObject = JWSObject.parse(jwtToken);
    JWK jwk = extractAndValidateJWK(jwsObject);
    verifySignature(jwsObject, (ECKey) jwk);
    validatePayloadHash(jwsObject.getPayload().toString(), concatenatedPayload);
    return jwk;
}
```

**Recommendation**: Migrate to Spring Security's native JWT support with the OAuth2 Resource Server:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

```java
// Modern implementation using Spring Security
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        return http.build();
    }
    
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Custom claims extraction logic
            return extractAuthorities(jwt);
        });
        return converter;
    }
}
```

### 2.2. Traditional JDBC/JPA Usage

**Issue**: The application uses traditional JPA repositories, which are blocking and can limit scalability under high load.

```java
// Current repository approach
@Repository
public interface OtpRequestRepository extends JpaRepository<OtpEntity, UUID> {
    Optional<OtpEntity> findByPublicKeyHash(String publicKeyHash);
    
    @Modifying
    @Query("UPDATE OtpEntity o SET o.otpCode = :otpCode, o.status = :status, o.updatedAt = :updatedAt WHERE o.publicKeyHash = :publicKeyHash")
    int updateOtpByPublicKeyHash(
            @Param("publicKeyHash") String publicKeyHash,
            @Param("otpCode") String otpCode,
            @Param("status") OtpStatus status,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
```

**Recommendation**: Consider adopting Spring Data R2DBC for reactive database operations:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
</dependency>
```

```java
// Modern reactive repository
@Repository
public interface ReactiveOtpRequestRepository extends ReactiveCrudRepository<OtpEntity, UUID> {
    Mono<OtpEntity> findByPublicKeyHash(String publicKeyHash);
    
    @Modifying
    @Query("UPDATE otp_requests SET otp = :otpCode, status = :status, updated_at = :updatedAt WHERE public_key_hash = :publicKeyHash")
    Mono<Integer> updateOtpByPublicKeyHash(
            String publicKeyHash,
            String otpCode,
            String status,
            LocalDateTime updatedAt
    );
}