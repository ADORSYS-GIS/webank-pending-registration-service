package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.service.DeviceRegServiceApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class DeviceRegRestServer implements DeviceRegRestApi {

    private final DeviceRegServiceApi deviceRegServiceApi;

    public DeviceRegRestServer(DeviceRegServiceApi deviceRegServiceApi) {
        this.deviceRegServiceApi = deviceRegServiceApi;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> initiateDeviceRegistration(String authorizationHeader, DeviceRegInitRequest regInitRequest) {

        return ResponseEntity.ok(deviceRegServiceApi.initiateDeviceRegistration(regInitRequest));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> validateDeviceRegistration(String authorizationHeader, DeviceValidateRequest deviceValidateRequest) {
        try {
            return ResponseEntity.ok(deviceRegServiceApi.validateDeviceRegistration(deviceValidateRequest));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Device validation failed: " + e.getMessage());
        }
    }
}
