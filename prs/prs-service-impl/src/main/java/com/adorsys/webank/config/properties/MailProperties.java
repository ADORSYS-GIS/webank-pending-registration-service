package com.adorsys.webank.config.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Properties;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {

    @NotBlank
    private String host;

    @NotNull
    private Integer port;

    @Email
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private Properties properties = new Properties();
}
