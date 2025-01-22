package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.service.DeviceRegServiceApi;
import org.springframework.stereotype.Service;

@Service
public class DeviceRegServiceImpl implements DeviceRegServiceApi {

    @Override
    public String initiateDeviceRegistration(String jwtToken, DeviceRegInitRequest regInitRequest) {

        return null;
    }
}
