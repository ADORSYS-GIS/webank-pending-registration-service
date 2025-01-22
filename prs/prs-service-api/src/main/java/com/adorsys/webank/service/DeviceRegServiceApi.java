package com.adorsys.webank.service;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import org.springframework.stereotype.Service;

@Service
public interface DeviceRegServiceApi {

    String initiateDeviceRegistration(String jwtToken, DeviceRegInitRequest regInitRequest);
}
