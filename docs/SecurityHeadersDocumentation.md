# Security Headers Configuration

This document provides a comprehensive overview of the security headers configuration in the Webank Pending Registration Service. The configuration is implemented in the `SecurityHeadersConfig` class and is designed to enhance the security of HTTP responses by setting up various security headers.

## Overview

The security headers configuration is implemented as a Spring Boot configuration class that utilizes Spring Security's header configuration capabilities. It is designed to be flexible and configurable through application properties, allowing security settings to be adjusted without modifying code.

The main components are:

1. `SecurityHeadersConfig` - The main configuration class that sets up security headers
2. `SecurityHeadersProperties` - Configuration properties class that holds all security header settings
3. Various security headers (CSP, XSS Protection, Frame Options, etc.) that can be configured through application properties

## Key Components

### 1. SecurityHeadersConfig Class

Located at: `com.adorsys.webank.security.SecurityHeadersConfig`

This class implements the following security headers:

1. Content Security Policy (CSP)
2. X-XSS-Protection
3. X-Frame-Options
4. Referrer-Policy
5. Permissions-Policy
6. HTTP Strict Transport Security (HSTS)

Each header can be configured independently through application properties.

### 2. SecurityHeadersProperties Class

Located at: `com.adorsys.webank.security.SecurityHeadersProperties`

This class holds all the configuration properties for the security headers and is annotated with `@ConfigurationProperties(prefix = "security.headers")`. It provides default values and allows customization through application.properties or application.yml.

## Configuration Properties

The security headers can be configured through the following properties:

```yaml
security:
  headers:
    enabled: true  # Enable/disable all security headers
    csp:
      policy-directives: "default-src 'self'"  # Content Security Policy directives
    x-frame-options: "SAMEORIGIN"  # X-Frame-Options value
    x-xss-protection: "1; mode=block"  # X-XSS-Protection value
    referrer-policy: "strict-origin-when-cross-origin"  # Referrer-Policy value
    permissions-policy: "geolocation=(), microphone=()"  # Permissions-Policy value
    hsts:
      max-age-in-seconds: 31536000  # 1 year
      include-sub-domains: true
```

## Security Header Details

### Content Security Policy (CSP)

Purpose: Prevents XSS attacks by controlling which resources can be loaded

- Default: Only allows resources from the same origin
- Example: `default-src 'self'; script-src 'self' https://trusted.cdn.com`
- Configurable through: `security.headers.csp.policy-directives`

### X-XSS-Protection

Purpose: Enables browser's XSS auditor

- Default: `1; mode=block` (blocks potentially unsafe content)
- Values: `0` (disabled), `1` (enabled), `1; mode=block` (block unsafe content)
- Configurable through: `security.headers.x-xss-protection`

### X-Frame-Options

Purpose: Prevents clickjacking attacks

- Default: `SAMEORIGIN` (only allows framing from same origin)
- Values: `DENY`, `SAMEORIGIN`, `ALLOW-FROM uri`
- Configurable through: `security.headers.x-frame-options`

### Referrer-Policy

Purpose: Controls referrer information sent with requests

- Default: `strict-origin-when-cross-origin`
- Values: `no-referrer`, `no-referrer-when-downgrade`, `origin`, `origin-when-cross-origin`, `same-origin`, `strict-origin`, `strict-origin-when-cross-origin`
- Configurable through: `security.headers.referrer-policy`

### Permissions-Policy

Purpose: Controls browser features and APIs

- Default: Empty (no restrictions)
- Example: `geolocation=(), microphone=()`, `camera=()`
- Configurable through: `security.headers.permissions-policy`

### HSTS

Purpose: Forces HTTPS usage

- Default: 1 year max-age with subdomain inclusion
- Configurable through: `security.headers.hsts.max-age-in-seconds` and `security.headers.hsts.include-sub-domains`

## Usage Examples

1. Enable/Disable Security Headers
```yaml
security:
  headers:
    enabled: true  # Set to false to disable all security headers
```

2. Configure Specific Headers
```yaml
security:
  headers:
    csp:
      policy-directives: "default-src 'self' https://api.example.com"
    referrer-policy: "strict-origin"
    permissions-policy: "geolocation=(), microphone=(), camera=()"
```

3. Configure HSTS
```yaml
security:
  headers:
    hsts:
      max-age-in-seconds: 86400  # 1 day
      include-sub-domains: false
```

## Best Practices

1. Always test security headers in a staging environment before deploying to production
2. Use the most restrictive policies that still allow your application to function
3. Regularly review and update security policies
4. Consider using a security header testing tool to verify configurations
5. Document any exceptions or special cases in your security policy

## Security Considerations

1. CSP is the most important header for preventing XSS attacks
2. HSTS should only be enabled after ensuring HTTPS works correctly
3. X-Frame-Options should be configured based on your application's needs for embedding
4. Referrer-Policy should balance security with functionality requirements
5. Permissions-Policy should be as restrictive as possible while maintaining functionality

## Troubleshooting

1. If resources are blocked by CSP:
   - Check browser console for CSP violation messages
   - Update policy directives to include required domains
   - Consider using report-uri or report-to for CSP violations

2. If HSTS is causing issues:
   - Check if HTTPS is properly configured
   - Verify certificate chain is valid
   - Consider reducing max-age for testing purposes

3. If X-Frame-Options is causing issues:
   - Review application requirements for embedding
   - Adjust policy if necessary
   - Consider using Content Security Policy frame-ancestors directive instead

## References

- [OWASP Secure Headers Project](https://owasp.org/www-project-secure-headers/)
- [MDN Web Docs - HTTP Headers](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [Content Security Policy Guide](https://content-security-policy.com/)