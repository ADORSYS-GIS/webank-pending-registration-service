package com.adorsys.webank;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
 
@TestConfiguration
@EnableMethodSecurity(prePostEnabled = false, securedEnabled = false, jsr250Enabled = false)
public class NoMethodSecurityConfig {
} 