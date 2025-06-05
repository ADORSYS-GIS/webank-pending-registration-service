package com.adorsys.webank.config;

import com.adorsys.webank.security.*;
import org.springframework.context.annotation.*;

import java.util.*;

@Configuration
public class EndpointConfig {

    @Bean
    public EndpointParameterMapper endpointParameterMapper() {
         final Map<String, List<String>> ENDPOINT_PARAMETERS = new HashMap<>();

        // Device Registration
        ENDPOINT_PARAMETERS.put("api/prs/dev/init", List.of("timeStamp"));
        ENDPOINT_PARAMETERS.put("api/prs/dev/validate", java.util.Arrays.asList("initiationNonce", "powHash", "powNonce"));

        // OTP
        ENDPOINT_PARAMETERS.put("api/prs/otp/send", List.of("phoneNumber"));
        ENDPOINT_PARAMETERS.put("api/prs/otp/validate", Arrays.asList("phoneNumber", "otpInput"));


        // Email OTP
        ENDPOINT_PARAMETERS.put("api/prs/email-otp/send", Arrays.asList("email", "accountId"));
        ENDPOINT_PARAMETERS.put("api/prs/email-otp/validate", Arrays.asList("email", "otpInput", "accountId"));

        // KYC
        ENDPOINT_PARAMETERS.put("api/prs/kyc/location", Arrays.asList("location", "accountId"));
        ENDPOINT_PARAMETERS.put("api/prs/kyc/info", Arrays.asList("idNumber", "expiryDate", "accountId"));
        ENDPOINT_PARAMETERS.put("api/prs/kyc/documents", Arrays.asList("frontId", "backId", "selfieId", "taxId", "accountId"));
        ENDPOINT_PARAMETERS.put("api/prs/kyc/record", List.of("accountId"));
        ENDPOINT_PARAMETERS.put("api/prs/kyc/findById/{DocumentUniqueId}", List.of("DocumentUniqueId"));


        return EndpointParameterMapper.builder().endpointParameters(ENDPOINT_PARAMETERS).build();


    }
}