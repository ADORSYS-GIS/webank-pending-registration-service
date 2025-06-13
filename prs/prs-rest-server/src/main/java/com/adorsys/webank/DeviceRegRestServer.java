package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;

import com.adorsys.webank.dto.response.DeviceResponse;
import com.adorsys.webank.dto.response.DeviceValidationResponse;
import com.adorsys.webank.service.DeviceRegServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
public class DeviceRegRestServer implements DeviceRegRestApi {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegRestServer.class);
    private final DeviceRegServiceApi deviceRegServiceApi;

    @Override
    public ResponseEntity<DeviceResponse> initiateDeviceRegistration(String authorizationHeader, DeviceRegInitRequest regInitRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received device registration initiation request [correlationId={}]", correlationId);
        
        try {
            log.debug("Processing device registration initiation [correlationId={}]", correlationId);
            DeviceResponse response = deviceRegServiceApi.initiateDeviceRegistration(regInitRequest);
            log.info("Device registration initiation completed successfully [correlationId={}]", correlationId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during device registration initiation [correlationId={}]", correlationId, e);
            DeviceResponse errorResponse = new DeviceResponse();
            errorResponse.setStatus(DeviceResponse.InitStatus.FAILED);
            errorResponse.setTimestamp(java.time.LocalDateTime.now());
            errorResponse.setMessage("Device registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Override
    public ResponseEntity<DeviceValidationResponse> validateDeviceRegistration(String authorizationHeader, DeviceValidateRequest deviceValidateRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received device registration validation request [correlationId={}]", correlationId);
        
        try {
            log.debug("Processing device registration validation [correlationId={}]", correlationId);
            DeviceValidationResponse response = deviceRegServiceApi.validateDeviceRegistration(deviceValidateRequest);
            log.info("Device registration validation completed successfully [correlationId={}]", correlationId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during device registration validation [correlationId={}]", correlationId, e);
            DeviceValidationResponse errorResponse = new DeviceValidationResponse();
            errorResponse.setStatus(DeviceValidationResponse.ValidationStatus.FAILED);
            errorResponse.setTimestamp(java.time.LocalDateTime.now());
            errorResponse.setMessage("Device validation failed: " + e.getMessage());
            errorResponse.setCertificate(null);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

