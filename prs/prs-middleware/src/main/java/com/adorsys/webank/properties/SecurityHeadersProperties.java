package com.adorsys.webank.properties;

import lombok.*;
import org.springframework.boot.context.properties.*;
import org.springframework.stereotype.*;

@Data
@Component
@ConfigurationProperties(prefix = "security.headers")
public class SecurityHeadersProperties {

    private boolean enabled = true;

    private ContentSecurityPolicy csp = new ContentSecurityPolicy();
    private String xFrameOptions;
    private String xXssProtection;
    private String referrerPolicy;
    private String permissionsPolicy;
    private Hsts hsts = new Hsts();

    @Data
    public static class ContentSecurityPolicy {
        private String policyDirectives;
    }

    @Data
    public static class Hsts {
        private int maxAgeInSeconds = 31536000; // 1 year
        private boolean includeSubDomains = true;
    }
}