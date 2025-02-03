package com.adorsys.webank;

import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.DeviceRegServiceApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.proc.BadJOSEException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;


@RestController
public class DeviceRegRestServer  implements  DeviceRegRestApi{
    private final DeviceRegServiceApi deviceRegServiceApi;

    public DeviceRegRestServer( DeviceRegServiceApi deviceRegServiceApi){
        this.deviceRegServiceApi=deviceRegServiceApi;
    }



    @Override
    public ResponseEntity<String> initiateDeviceRegistration(String authorizationHeader, DeviceRegInitRequest regInitRequest) {

        return ResponseEntity.ok(deviceRegServiceApi.initiateDeviceRegistration( regInitRequest));
    }

    @Override
    public ResponseEntity<String> validateDeviceRegistration(String authorizationHeader, DeviceValidateRequest deviceValidateRequest) throws BadJOSEException, ParseException, NoSuchAlgorithmException, JOSEException, IOException {
        String jwtToken;
        JWK publicKey;
        jwtToken = extractJwtFromHeader(authorizationHeader);
        publicKey = JwtValidator.validateAndExtract(jwtToken);

        return ResponseEntity.ok(deviceRegServiceApi.validateDeviceRegistration(publicKey, deviceValidateRequest));

    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}