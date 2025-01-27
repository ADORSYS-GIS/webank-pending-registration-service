package com.adorsys.webank.dto;

import lombok.Data;

@Data
public class DeviceValidateRequest {

    private String initiationNonce;
    private String powHash;
    private String powNonce;

}
