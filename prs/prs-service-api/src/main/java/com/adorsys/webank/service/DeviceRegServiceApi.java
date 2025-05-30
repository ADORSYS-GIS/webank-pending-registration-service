package com.adorsys.webank.service;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;

import java.io.IOException;

public interface DeviceRegServiceApi {


    String initiateDeviceRegistration(DeviceRegInitRequest regInitRequest);

    String validateDeviceRegistration(DeviceValidateRequest deviceValidateRequest) throws IOException;
}
