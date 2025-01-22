package com.adorsys.webank;

import com.adorsys.webank.service.DeviceRegServiceApi;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class DeviceRegRestServer  implements  DeviceRegRestApi{
    private final DeviceRegServiceApi deviceRegServiceApi;

   public DeviceRegRestServer( DeviceRegServiceApi deviceRegServiceApi){

       this.deviceRegServiceApi=deviceRegServiceApi;
   }


    @Override
    public String initiateDeviceRegistration(String jwtToken) {
        return deviceRegServiceApi.initiateDeviceRegistration(jwtToken);
    }
}
