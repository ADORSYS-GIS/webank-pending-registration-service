package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.dto.response.DeviceResponse;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.DeviceRegServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class DeviceRegRestServer implements DeviceRegRestApi {
    private final DeviceRegServiceApi deviceRegServiceApi;

    public DeviceRegRestServer(DeviceRegServiceApi deviceRegServiceApi) {
        this.deviceRegServiceApi = deviceRegServiceApi;
    }

    @Override
    public ResponseEntity<DeviceResponse> initDeviceRegistration(String authorizationHeader, DeviceRegInitRequest request) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);

            String timeStamp = request.getTimeStamp();// Extract the timeStamp

            // Validate the JWT token
            publicKey = JwtValidator.validateAndExtract(jwtToken, timeStamp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid JWT: " + e.getMessage()));
        }
        
        String result = deviceRegServiceApi.initiateDeviceRegistration(publicKey, request);
        
        DeviceResponse response = new DeviceResponse();
        response.setDeviceId("dev_" + System.currentTimeMillis());
        response.setStatus(DeviceResponse.DeviceStatus.INITIALIZED);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(result);
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DeviceResponse> validateDevice(String authorizationHeader, DeviceValidateRequest request) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);

            String initiationNonce = request.getInitiationNonce();
            String powHash = request.getPowHash();
            String powNonce = request.getPowNonce();

            publicKey = JwtValidator.validateAndExtract(jwtToken, initiationNonce, powHash, powNonce);
            
            String result = deviceRegServiceApi.validateDeviceRegistration(publicKey, request);
            
            DeviceResponse response = new DeviceResponse();
            response.setDeviceId("dev_" + System.currentTimeMillis());
            response.setStatus(DeviceResponse.DeviceStatus.VALIDATED);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(result);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid JWT: " + e.getMessage()));
        }
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
    
    private DeviceResponse createErrorResponse(String message) {
        DeviceResponse response = new DeviceResponse();
        response.setDeviceId("error_" + System.currentTimeMillis());
        response.setStatus(DeviceResponse.DeviceStatus.FAILED);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("Error: " + message);
        return response;
    }
}