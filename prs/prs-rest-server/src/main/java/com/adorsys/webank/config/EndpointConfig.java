package com.adorsys.webank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class EndpointConfig {

    @Bean
    public com.adorsys.webank.security.extractor.EndpointParameterMapper endpointParameterMapper() {
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
        ENDPOINT_PARAMETERS.put("api/prs/kyc/status/update", Arrays.asList("idNumber", "expiryDate", "accountId", "status"));

        // KYC Recovery
        ENDPOINT_PARAMETERS.put("api/prs/kyc/recovery/verify", Arrays.asList("accountId", "idNumber", "expiryDate"));
        ENDPOINT_PARAMETERS.put("api/prs/kyc/recovery/token", Arrays.asList("oldAccountId", "newAccountId"));
        ENDPOINT_PARAMETERS.put("api/prs/kyc/recovery/validate", List.of("newAccountId"));

        return com.adorsys.webank.security.extractor.EndpointParameterMapper.builder().endpointParameters(ENDPOINT_PARAMETERS).build();
    }
}