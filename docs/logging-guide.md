# WeBank PRS Comprehensive Logging Strategy

## Overview

This guide documents the comprehensive logging strategy implemented in the WeBank Pending Registration Service. The implementation provides:

- **Structured JSON logging** for production environments
- **Correlation ID tracking** across request lifecycles
- **Sensitive data masking** to protect user information
- **Environment-specific configurations** for development and production
- **Appropriate log level usage** for different types of events

This implementation addresses the need for improved observability and debugging capabilities, especially in production environments.

## Required Dependencies

The logging implementation requires the following dependencies in your `pom.xml`:

```xml
<!-- SLF4J API -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>${slf4j.version}</version>
</dependency>

<!-- Logback implementation -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>${logback.version}</version>
</dependency>

<!-- Logstash encoder for JSON formatting -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### Purpose of Dependencies

1. **slf4j-api**: Provides the logging facade that allows switching between different logging implementations without changing the code.
2. **logback-classic**: The actual logging implementation used behind the SLF4J facade.
3. **logstash-logback-encoder**: Enables JSON-formatted logs that are easily parsed by log analysis tools like ELK stack (Elasticsearch, Logstash, Kibana).

## Configuration Overview

The application uses two primary configuration profiles:

1. **H2 Profile (Development)**
   ```
   -Dspring.profiles.active=h2
   ```

2. **Postgres Profile (Production)**
   ```
   -Dspring.profiles.active=postgres
   ```

## Correlation ID Implementation

Correlation IDs allow tracking a request throughout its entire lifecycle across multiple components. This is implemented through:

1. **Filter Implementation**: `CorrelationIdFilter` adds a unique ID to each incoming request
   ```java
   @Component
   @Order(Ordered.HIGHEST_PRECEDENCE)
   public class CorrelationIdFilter implements Filter {
       @Override
       public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
           HttpServletRequest httpRequest = (HttpServletRequest) request;
           
           // Get existing ID or generate a new one
           String correlationId = httpRequest.getHeader("X-Correlation-ID");
           if (correlationId == null || correlationId.isEmpty()) {
               correlationId = UUID.randomUUID().toString();
           }
           
           // Store in MDC for logging
           MDC.put("correlationId", correlationId);
           
           try {
               chain.doFilter(request, response);
           } finally {
               MDC.remove("correlationId");
           }
       }
   }
   ```

2. **Usage in Controllers**: All controllers include the correlation ID in logs
   ```java
   String correlationId = MDC.get("correlationId");
   log.info("Processing request [correlationId={}]", correlationId);
   ```

3. **Configuration in logback.xml**: The pattern includes correlation ID from MDC
   ```xml
   <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{correlationId}] %-5level %logger{36} - %msg%n</pattern>
   ```

## Logging Levels

The application uses these standard log levels:

| Level | Usage                                          | Examples                                   |
|-------|------------------------------------------------|--------------------------------------------|
| ERROR | Critical errors requiring immediate attention   | Connection failures, service exceptions    |
| WARN  | Potential issues that might need investigation  | Validation failures, token expirations     | 
| INFO  | Key operational events                          | Request start/end, successful operations   |
| DEBUG | Detailed information for troubleshooting        | Method entry/exit, data processing steps   |

## How to Add Logging to Your Code

### 1. Create a Logger

Add a logger instance to your class:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyService {
    private static final Logger log = LoggerFactory.getLogger(MyService.class);
    
    // Class implementation
}
```

### 2. Log Method Entry/Exit

```java
@Override
public String processData(String data) {
    log.info("Processing data");
    
    // Method implementation
    
    log.info("Data processing completed");
    return result;
}
```

### 3. Mask Sensitive Data

Always mask sensitive information in logs:

```java
log.info("Processing request for user: {}", maskEmail(email));
```

Use existing masking utilities or create new ones as needed:

```java
private String maskEmail(String email) {
    if (email == null || !email.contains("@")) {
        return "********";
    }
    int atIndex = email.indexOf('@');
    return email.substring(0, 1) + "****" + email.substring(atIndex);
}
```

### 4. Include Correlation IDs

For REST controllers, the correlation ID is automatically added to the MDC by the CorrelationIdFilter.

When logging in controllers, include the correlation ID:

```java
String correlationId = MDC.get("correlationId");
log.info("Processing request [correlationId={}]", correlationId);
```

### 5. Use Appropriate Log Levels

- **ERROR**: Use for exceptions and critical failures
  ```java
  try {
      // Operation
  } catch (Exception e) {
      log.error("Failed to process transaction", e);
      throw new ServiceException("Transaction failed", e);
  }
  ```

- **WARN**: Use for potential issues
  ```java
  if (token.isNearExpiration()) {
      log.warn("Token is about to expire for user: {}", maskUserId(userId));
  }
  ```

- **INFO**: Use for operational events
  ```java
  log.info("User authenticated successfully");
  ```

- **DEBUG**: Use for detailed troubleshooting information
  ```java
  log.debug("Request payload: {}", maskSensitiveData(payload));
  ```

## Testing Your Logging

### Local Testing

Run the application with the development profile:

```bash
./mvnw spring-boot:run -Dspring.profiles.active=h2
```

Make API requests and check the console for logs.

### Viewing Detailed Logs

To see more detailed logs for a specific component, adjust its level in `logback-spring.xml`:

```xml
<logger name="com.adorsys.webank.serviceimpl.MyService" level="DEBUG"/>
```

Or set it temporarily via application properties:

```
logging.level.com.adorsys.webank.serviceimpl.MyService=DEBUG
```

## Log Format

### Development (Console)

In development, logs are output in a human-readable format:

```
2025-06-03 15:23:08.089 [http-nio-8080-exec-7] [735a949b-fab0-49bd-9c48-2b0a6a862346] INFO c.a.w.serviceimpl.OtpServiceImpl - Processing OTP send request for phone: ******6316
```

### Production (JSON)

In production, logs are output in JSON format:

```json
{
  "timestamp": "2025-06-03T15:23:08.089Z",
  "level": "INFO",
  "thread": "http-nio-8080-exec-7",
  "logger_name": "com.adorsys.webank.serviceimpl.OtpServiceImpl",
  "message": "Processing OTP send request for phone: ******6316",
  "correlationId": "735a949b-fab0-49bd-9c48-2b0a6a862346",
  "application": "webank-prs"
}
```

## Best Practices

1. **Never log sensitive data unmasked** (passwords, OTPs, PII)
2. **Include context in log messages** to make them useful for troubleshooting
3. **Use structured format** for complex data instead of string concatenation
4. **Log both success and failure cases** at appropriate levels
5. **Clean up MDC context** at the end of request processing

## Common Issues and Solutions

### 1. Missing Correlation ID

**Problem**: Logs don't include correlation ID.

**Solution**: Make sure your controller is properly reading it from MDC:

```java
String correlationId = MDC.get("correlationId");
log.info("Operation completed [correlationId={}]", correlationId);
```

### 2. Excessive Logging

**Problem**: Too many logs making it hard to find important information.

**Solution**: Review log levels and adjust accordingly:
- Move detailed operation steps to DEBUG level
- Keep only key business events at INFO level

### 3. Missing Context in Error Logs

**Problem**: Error logs don't provide enough context to troubleshoot.

**Solution**: Include relevant information when logging errors:

```java
log.error("Failed to process payment for orderId: {} with amount: {}", 
        maskOrderId(orderId), amount, exception);
``` 