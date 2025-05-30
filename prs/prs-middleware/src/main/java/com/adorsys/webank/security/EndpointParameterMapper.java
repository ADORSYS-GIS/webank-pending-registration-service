package com.adorsys.webank.security;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class EndpointParameterMapper {
    private static final Map<String, List<String>> ENDPOINT_PARAMETERS = new HashMap<>();

    static {
        // Device Registration
        ENDPOINT_PARAMETERS.put("api/prs/dev/init", List.of("timeStamp"));
        ENDPOINT_PARAMETERS.put("api/prs/dev/validate", Arrays.asList("initiationNonce", "powHash", "powNonce"));
        
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
        ENDPOINT_PARAMETERS.put("api/prs/kyc/findById/*", List.of("DocumentUniqueId)"));

    }

    public List<String> getRequiredParameters(String path) {
        return ENDPOINT_PARAMETERS.getOrDefault(path, Collections.emptyList());
    }
}
