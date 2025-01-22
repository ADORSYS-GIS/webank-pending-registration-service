package com.adorsys.webank.service;

import org.springframework.stereotype.Service;

@Service
public interface DeviceRegServiceApi {

    String initiateDeviceRegistration(String jwtToken);
}
