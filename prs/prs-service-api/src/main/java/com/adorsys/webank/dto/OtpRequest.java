package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpRequest {
    private String phoneNumber;

    public OtpRequest(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

}
