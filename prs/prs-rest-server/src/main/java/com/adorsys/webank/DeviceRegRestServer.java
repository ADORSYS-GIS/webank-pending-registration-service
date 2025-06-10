package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.service.DeviceRegServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DeviceRegRestServer implements DeviceRegRestApi {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegRestServer.class);
    private final DeviceRegServiceApi deviceRegServiceApi;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> initiateDeviceRegistration(String authorizationHeader, DeviceRegInitRequest regInitRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received device registration initiation request [correlationId={}]", correlationId);
        
        try {
            log.debug("Processing device registration initiation [correlationId={}]", correlationId);
            String result = deviceRegServiceApi.initiateDeviceRegistration(regInitRequest);
            log.info("Device registration initiation completed successfully [correlationId={}]", correlationId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during device registration initiation [correlationId={}]", correlationId, e);
            return ResponseEntity.badRequest().body("Device registration failed: " + e.getMessage());
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> validateDeviceRegistration(String authorizationHeader, DeviceValidateRequest deviceValidateRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received device registration validation request [correlationId={}]", correlationId);
        
        try {
            log.debug("Processing device registration validation [correlationId={}]", correlationId);
            String result = deviceRegServiceApi.validateDeviceRegistration(deviceValidateRequest);
            log.info("Device registration validation completed successfully [correlationId={}]", correlationId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during device registration validation [correlationId={}]", correlationId, e);
            return ResponseEntity.badRequest().body("Device validation failed: " + e.getMessage());
        }
    }
}
