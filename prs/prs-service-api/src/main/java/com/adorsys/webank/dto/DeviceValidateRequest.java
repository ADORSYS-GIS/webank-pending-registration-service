package com.adorsys.webank.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class DeviceValidateRequest {

    private String initiationNonce;
    private String powHash;
    private String powNonce;

}
