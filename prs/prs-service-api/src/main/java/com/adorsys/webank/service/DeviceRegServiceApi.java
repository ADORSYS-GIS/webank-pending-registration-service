package com.adorsys.webank.service;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public interface DeviceRegServiceApi {

    ResponseEntity<String> initiateDeviceRegistration(String jwtToken, DeviceRegInitRequest regInitRequest);

    ResponseEntity<String> validateDeviceRegistration(String jwtToken, DeviceValidateRequest deviceValidateRequest);

}
