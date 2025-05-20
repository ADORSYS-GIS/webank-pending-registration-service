## 7. Spring Boot Best Practices

### 7.1. Configuration Management

**Issue**: The application uses property files with hardcoded values and environment variables without proper validation or documentation.

**Recommendation**: Implement a comprehensive configuration management approach:

```java
@ConfigurationProperties(prefix = "app.security")
@Validated
@Data
public class SecurityProperties {
    
    @NotBlank(message = "JWT issuer must be specified")
    private String jwtIssuer;
    
    @Min(value = 60000, message = "JWT expiration time must be at least 60 seconds")
    private long jwtExpirationTimeMs;
    
    @NotBlank(message = "Server private key must be specified")
    private String serverPrivateKey;
    
    @NotBlank(message = "Server public key must be specified")
    private String serverPublicKey;
    
    @NotBlank(message = "OTP salt must be specified")
    private String otpSalt;
}

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
    
    private final SecurityProperties securityProperties;
    
    public SecurityConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }
    
    // Configuration beans using properties
}
```

### 7.2. Lack of Actuator Endpoints Customization

**Issue**: The application exposes Actuator endpoints without proper customization or security.

**Recommendation**: Implement a comprehensive Actuator configuration:

```java
@Configuration
public class ActuatorConfig {
    
    @Bean
    public InfoContributor buildInfoContributor() {
        return builder -> {
            builder.withDetail("version", getBuildVersion());
            builder.withDetail("buildTime", getBuildTime());
            builder.withDetail("environment", getEnvironment());
        };
    }
    
    @Bean
    public HealthContributor databaseHealthContributor(DataSource dataSource) {
        return new DataSourceHealthIndicator(dataSource, "SELECT 1");
    }
    
    @Bean
    public WebSecurityCustomizer actuatorSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/api/prs/actuator/health/**", "/api/prs/actuator/info");
    }
}
```

### 7.3. Lack of Proper Logging Strategy

**Issue**: The application lacks a comprehensive logging strategy, making it difficult to troubleshoot issues in production.

**Recommendation**: Implement a structured logging approach with correlation IDs:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

Create a `logback-spring.xml` configuration:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <springProperty scope="context" name="appName" source="spring.application.name" defaultValue="prs"/>
    <springProperty scope="context" name="appProfile" source="spring.profiles.active" defaultValue="default"/>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>correlationId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <fieldNames>
                <timestamp>timestamp</timestamp>
                <thread>thread</thread>
                <logger>logger</logger>
                <level>level</level>
                <message>message</message>
            </fieldNames>
            <customFields>{"app":"${appName}","profile":"${appProfile}"}</customFields>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
    
    <logger name="com.adorsys.webank" level="DEBUG"/>
</configuration>
```

Implement a filter to add correlation IDs:

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
```

## 8. Modern Spring Boot Features

### 8.1. Lack of Reactive Programming

**Issue**: The application uses a traditional blocking approach, which limits scalability under high load.

**Recommendation**: Adopt Spring WebFlux for reactive programming:

```java
// Current controller approach
@RestController
@RequestMapping("/api/prs/otp")
public class OtpRestServer implements OtpRestApi {
    
    @PostMapping("/send")
    public String sendOtp(@RequestHeader("Authorization") String authHeader, @RequestBody OtpRequest request) {
        // Blocking implementation
    }
}

// Modern reactive controller
@RestController
@RequestMapping("/api/prs/otp")
public class ReactiveOtpController {
    
    private final ReactiveOtpService otpService;
    
    @PostMapping("/send")
    public Mono<String> sendOtp(@RequestHeader("Authorization") String authHeader, @RequestBody OtpRequest request) {
        return Mono.just(authHeader)
            .map(this::extractJwtFromHeader)
            .flatMap(jwt -> validateJwt(jwt, request.getPhoneNumber()))
            .flatMap(publicKey -> otpService.sendOtp(publicKey, request.getPhoneNumber()));
    }
}
```

### 8.2. Lack of Native Image Support

**Issue**: The application does not leverage GraalVM native image capabilities for faster startup and lower memory footprint.

**Recommendation**: Configure the application for GraalVM native image compilation:

Create a `src/main/resources/META-INF/native-image/reflect-config.json` file:

```json
[
  {
    "name": "com.adorsys.webank.domain.OtpEntity",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true,
    "allDeclaredFields": true,
    "allPublicFields": true
  },
  {
    "name": "com.adorsys.webank.domain.PersonalInfoEntity",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true,
    "allDeclaredFields": true,
    "allPublicFields": true
  }
]
```

Update the `pom.xml` with native image plugin:

```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <configuration>
        <classesDirectory>${project.build.outputDirectory}</classesDirectory>
        <metadataRepository>
            <enabled>true</enabled>
        </metadataRepository>
        <buildArgs>
            <buildArg>--no-fallback</buildArg>
            <buildArg>--initialize-at-build-time=ch.qos.logback</buildArg>
        </buildArgs>
    </configuration>
</plugin>
```

### 8.3. Lack of Observability Features

**Issue**: The application lacks comprehensive observability features for monitoring and troubleshooting.

**Recommendation**: Implement Spring Boot 3.x+ Observability with Micrometer and OpenTelemetry:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```java
@Configuration
public class ObservabilityConfig {
    
    @Bean
    public ObservationRegistry observationRegistry(MeterRegistry meterRegistry) {
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().meter(meterRegistry);
        return registry;
    }
    
    @Bean
    public OtlpGrpcSpanExporter otlpGrpcSpanExporter(@Value("${opentelemetry.endpoint}") String endpoint) {
        return OtlpGrpcSpanExporter.builder()
            .setEndpoint(endpoint)
            .build();
    }
}