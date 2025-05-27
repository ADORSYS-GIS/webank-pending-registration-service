package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.DeviceRegServiceApi;
import com.nimbusds.jose.jwk.JWK;
import com.adorsys.error.JwtValidationException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceRegRestServer implements DeviceRegRestApi {
    private final DeviceRegServiceApi deviceRegServiceApi;

    public DeviceRegRestServer(DeviceRegServiceApi deviceRegServiceApi) {
        this.deviceRegServiceApi = deviceRegServiceApi;
    }

    @Override
    public ResponseEntity<String> initiateDeviceRegistration(String authorizationHeader, @Valid DeviceRegInitRequest regInitRequest) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);

            String timeStamp = regInitRequest.getTimeStamp();

            // Validate the JWT token
            publicKey = JwtValidator.validateAndExtract(jwtToken, timeStamp);
        } catch (IllegalArgumentException e) {
            throw new JwtValidationException("Invalid authorization header: " + e.getMessage());
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }
        return ResponseEntity.ok(deviceRegServiceApi.initiateDeviceRegistration(publicKey, regInitRequest));
    }

    @Override
    public ResponseEntity<String> validateDeviceRegistration(String authorizationHeader, @Valid DeviceValidateRequest deviceValidateRequest) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);

            String initiationNonce = deviceValidateRequest.getInitiationNonce();
            String powHash = deviceValidateRequest.getPowHash();
            String powNonce = deviceValidateRequest.getPowNonce();

            publicKey = JwtValidator.validateAndExtract(jwtToken, initiationNonce, powHash, powNonce);
            return ResponseEntity.ok(deviceRegServiceApi.validateDeviceRegistration(publicKey, deviceValidateRequest));

        } catch (IllegalArgumentException e) {
            throw new JwtValidationException("Invalid authorization header: " + e.getMessage());
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new JwtValidationException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}