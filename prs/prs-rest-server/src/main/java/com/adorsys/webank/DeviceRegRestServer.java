package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.service.DeviceRegServiceApi;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class DeviceRegRestServer  implements  DeviceRegRestApi{
    private final DeviceRegServiceApi deviceRegServiceApi;

   public DeviceRegRestServer( DeviceRegServiceApi deviceRegServiceApi){

       this.deviceRegServiceApi=deviceRegServiceApi;
   }



    @Override
    public String initiateDeviceRegistration(String jwtToken, DeviceRegInitRequest regInitRequest) {
        return deviceRegServiceApi.initiateDeviceRegistration(jwtToken, regInitRequest);
    }

    @Override
    public String validateDeviceRegistration(String jwtToken, DeviceValidateRequest deviceValidateRequest) {
       return deviceRegServiceApi.validateDeviceRegistration(jwtToken, deviceValidateRequest);
    }

}
