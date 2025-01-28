package com.adorsys.webank.service;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import org.springframework.stereotype.Service;

@Service
public interface DeviceRegServiceApi {

    String initiateDeviceRegistration(String jwtToken, DeviceRegInitRequest regInitRequest);

    String validateDeviceRegistration(String jwtToken, DeviceValidateRequest deviceValidateRequest);

}
