package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.DeviceRegServiceApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class DeviceRegRestServer  implements  DeviceRegRestApi{
    private final DeviceRegServiceApi deviceRegServiceApi;

   public DeviceRegRestServer( DeviceRegServiceApi deviceRegServiceApi){
       this.deviceRegServiceApi=deviceRegServiceApi;
   }



    @Override
    public ResponseEntity<String> initiateDeviceRegistration(String authorizationHeader, DeviceRegInitRequest regInitRequest) {
        String jwtToken;
       try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);

            // Validate the JWT token
            JwtValidator.validateAndExtract(jwtToken);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid JWT: " + e.getMessage());
        }
        return deviceRegServiceApi.initiateDeviceRegistration(jwtToken, regInitRequest);
    }

    @Override
    public ResponseEntity<String> validateDeviceRegistration(String authorizationHeader, DeviceValidateRequest deviceValidateRequest) {
        String jwtToken;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);

            // Validate the JWT token
            JwtValidator.validateAndExtract(jwtToken);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid JWT: " + e.getMessage());
        }
        return deviceRegServiceApi.validateDeviceRegistration(jwtToken, deviceValidateRequest);
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}
