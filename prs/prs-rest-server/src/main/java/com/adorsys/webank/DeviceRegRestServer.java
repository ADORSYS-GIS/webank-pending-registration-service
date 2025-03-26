package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.DeviceRegServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
class DeviceRegRestServer  implements  DeviceRegRestApi{
    private final DeviceRegServiceApi deviceRegServiceApi;

    public DeviceRegRestServer( DeviceRegServiceApi deviceRegServiceApi){
        this.deviceRegServiceApi=deviceRegServiceApi;
    }



    @Override
    public ResponseEntity<String> initiateDeviceRegistration(String authorizationHeader, DeviceRegInitRequest regInitRequest) {
        String jwtToken;
        JWK publicKey;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);

            String timeStamp = regInitRequest.getTimeStamp();// Extract the timeStamp

            // Validate the JWT token
            publicKey = JwtValidator.validateAndExtract(jwtToken, timeStamp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid JWT: " + e.getMessage());
        }
        return ResponseEntity.ok(deviceRegServiceApi.initiateDeviceRegistration(publicKey, regInitRequest));
    }

    @Override
    public ResponseEntity<String> validateDeviceRegistration(String authorizationHeader, DeviceValidateRequest deviceValidateRequest) {
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

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid JWT: " + e.getMessage());
        }
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}