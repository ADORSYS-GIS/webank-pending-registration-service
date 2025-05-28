package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.dto.response.DeviceResponse;
import com.adorsys.webank.dto.response.DeviceResponse.InitStatus;
import com.adorsys.webank.dto.response.DeviceValidationResponse;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.DeviceRegServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class DeviceRegRestServer implements DeviceRegRestApi {
    private static final Logger log = LoggerFactory.getLogger(DeviceRegRestServer.class);
    private final DeviceRegServiceApi deviceRegServiceApi;

    public DeviceRegRestServer(DeviceRegServiceApi deviceRegServiceApi) {
        this.deviceRegServiceApi = deviceRegServiceApi;
    }

    @Override
    public ResponseEntity<DeviceResponse> initiateDeviceRegistration(String authorizationHeader, DeviceRegInitRequest request) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("Processing device registration initialization request");

            String timeStamp = request.getTimeStamp();
            if (timeStamp == null || timeStamp.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Missing required timestamp"));
            }

            // Validate the JWT token
            publicKey = JwtValidator.validateAndExtract(jwtToken, timeStamp);
            log.info("JWT validation successful");
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Authentication failed: " + e.getMessage()));
        }
        
        try {
            String nonce = deviceRegServiceApi.initiateDeviceRegistration(publicKey, request);
            
            DeviceResponse response = new DeviceResponse();
            response.setStatus(InitStatus.INITIALIZED);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(nonce);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Device registration initialization failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to initialize device registration: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<DeviceValidationResponse> validateDeviceRegistration(String authorizationHeader, DeviceValidateRequest request) {
        try {
            // Extract the JWT token from the Authorization header
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("Processing device validation request");

            // Extract request parameters
            validateDeviceValidationRequest(request);
            
            // Validate the JWT token and get the public key
            JWK publicKey = JwtValidator.validateAndExtract(
                jwtToken, 
                request.getInitiationNonce(), 
                request.getPowHash(), 
                request.getPowNonce()
            );
            log.info("JWT validation successful");
            
            // Get the device certificate JWT from the service
            String deviceCertificate = deviceRegServiceApi.validateDeviceRegistration(publicKey, request);
            log.info("Generated device certificate: {}", deviceCertificate);
            
            // Create the validation response with the certificate
            DeviceValidationResponse response = new DeviceValidationResponse();
            response.setStatus(DeviceValidationResponse.ValidationStatus.VALIDATED);
            response.setTimestamp(LocalDateTime.now());
            response.setCertificate(deviceCertificate);
            response.setMessage(deviceCertificate);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Device validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createValidationErrorResponse("Validation failed: " + e.getMessage()));
        }
    }
    
    private void validateDeviceValidationRequest(DeviceValidateRequest request) {
        validateField(request.getInitiationNonce(), "initiation nonce");
        validateField(request.getPowHash(), "proof of work hash");
        validateField(request.getPowNonce(), "proof of work nonce");
    }
    
    private void validateField(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing " + fieldName);
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
        response.setStatus(InitStatus.FAILED);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("Error: " + message);
        return response;
    }
    
    private DeviceValidationResponse createValidationErrorResponse(String message) {
        DeviceValidationResponse response = new DeviceValidationResponse();
        response.setStatus(DeviceValidationResponse.ValidationStatus.FAILED);
        response.setTimestamp(LocalDateTime.now());
        response.setCertificate(null);
        response.setMessage("Error: " + message);
        return response;
    }
}