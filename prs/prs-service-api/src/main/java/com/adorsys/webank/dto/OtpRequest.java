package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OtpRequest {
    private String phoneNumber;

    public OtpRequest(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

}
