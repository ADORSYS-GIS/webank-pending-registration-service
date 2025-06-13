package com.adorsys.webank.service;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;

import java.io.IOException;
import com.adorsys.webank.dto.response.DeviceResponse;
import com.adorsys.webank.dto.response.DeviceValidationResponse;

public interface DeviceRegServiceApi {


    DeviceResponse initiateDeviceRegistration(DeviceRegInitRequest regInitRequest);

    DeviceValidationResponse validateDeviceRegistration(DeviceValidateRequest deviceValidateRequest) throws IOException;
}
