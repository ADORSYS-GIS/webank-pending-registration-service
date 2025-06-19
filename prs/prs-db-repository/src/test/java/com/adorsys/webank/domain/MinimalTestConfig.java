package com.adorsys.webank.domain;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootConfiguration
@EnableJpaRepositories(basePackages = "com.adorsys.webank.repository")
@EnableAutoConfiguration
public class MinimalTestConfig {
} 