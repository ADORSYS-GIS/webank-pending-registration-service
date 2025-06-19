package com.adorsys.webank.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "springdoc")
public class SpringDocProperties {

    private ApiDocs apiDocs = new ApiDocs();
    private SwaggerUi swaggerUi = new SwaggerUi();

    @NotBlank
    private String defaultProducesMediaType;

    @NotBlank
    private String defaultConsumesMediaType;

    @Data
    public static class ApiDocs {
        private boolean enabled;
        @NotBlank
        private String path;
    }

    @Data
    public static class SwaggerUi {
        private boolean enabled;
        @NotBlank
        private String path;
        @NotBlank
        private String operationsSorter;
        @NotBlank
        private String tagsSorter;
        private boolean tryItOutEnabled;
        private boolean filter;
        private boolean displayRequestDuration;
    }
}
