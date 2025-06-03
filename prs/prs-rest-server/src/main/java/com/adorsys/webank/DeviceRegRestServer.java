package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.DeviceRegServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class DeviceRegRestServer implements DeviceRegRestApi {
    private static final Logger log = LoggerFactory.getLogger(DeviceRegRestServer.class);
    private final DeviceRegServiceApi deviceRegServiceApi;

    public DeviceRegRestServer(DeviceRegServiceApi deviceRegServiceApi) {
        this.deviceRegServiceApi = deviceRegServiceApi;
    }

    @Override
    public ResponseEntity<String> initiateDeviceRegistration(String authorizationHeader, DeviceRegInitRequest regInitRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received device registration initiation request [correlationId={}]", correlationId);
        
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String timeStamp = regInitRequest.getTimeStamp();// Extract the timeStamp
            
            log.debug("Validating JWT token for device registration initiation [correlationId={}]", correlationId);
            
            // Validate the JWT token
            publicKey = JwtValidator.validateAndExtract(jwtToken, timeStamp);
            log.debug("JWT validation successful for device registration [correlationId={}]", correlationId);
        } catch (Exception e) {
            log.error("JWT validation failed for device registration [correlationId={}]", correlationId, e);
            return ResponseEntity.badRequest().body("Invalid JWT: " + e.getMessage());
        }
        
        log.info("Processing device registration initiation [correlationId={}]", correlationId);
        String result = deviceRegServiceApi.initiateDeviceRegistration(publicKey, regInitRequest);
        log.info("Device registration initiation completed successfully [correlationId={}]", correlationId);
        
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<String> validateDeviceRegistration(String authorizationHeader, DeviceValidateRequest deviceValidateRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received device registration validation request [correlationId={}]", correlationId);
        
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);

            String initiationNonce = deviceValidateRequest.getInitiationNonce();
            String powHash = deviceValidateRequest.getPowHash();
            String powNonce = deviceValidateRequest.getPowNonce();
            
            log.debug("Validating JWT token for device registration validation [correlationId={}]", correlationId);
            publicKey = JwtValidator.validateAndExtract(jwtToken, initiationNonce, powHash, powNonce);
            log.debug("JWT validation successful for device validation [correlationId={}]", correlationId);
            
            log.info("Processing device registration validation [correlationId={}]", correlationId);
            String result = deviceRegServiceApi.validateDeviceRegistration(publicKey, deviceValidateRequest);
            log.info("Device registration validation completed successfully [correlationId={}]", correlationId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during device registration validation [correlationId={}]", correlationId, e);
            return ResponseEntity.badRequest().body("Invalid JWT: " + e.getMessage());
        }
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Invalid authorization header format");
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}