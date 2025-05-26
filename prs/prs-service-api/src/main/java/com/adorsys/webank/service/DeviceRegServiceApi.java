package com.adorsys.webank.service;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface DeviceRegServiceApi {

    String initiateDeviceRegistration(JWK publicKey, DeviceRegInitRequest regInitRequest);

    String validateDeviceRegistration(JWK publicKey, DeviceValidateRequest deviceValidateRequest);

}
