package com.adorsys.webank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailOtpData {
    @JsonProperty("emailOtp")
    private String emailOtp;
    
    @JsonProperty("accountId")
    private String accountId;

    public static EmailOtpData create(String emailOtp, String accountId) {
        return new EmailOtpData(emailOtp, accountId);
    }
}
