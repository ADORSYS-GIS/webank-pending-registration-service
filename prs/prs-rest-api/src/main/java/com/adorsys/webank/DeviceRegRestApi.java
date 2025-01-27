package com.adorsys.webank;


import com.adorsys.webank.dto.DeviceRegInitRequest;
import com.adorsys.webank.dto.DeviceValidateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Device Registration", description = "Operations related to device registration processing")
@RequestMapping("/api/dev")
public interface DeviceRegRestApi {

    @Operation(summary = "send nonce", description = "Sends time base nonce  to the user's device for pow computaion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "nonce sent sucessfully"),
            @ApiResponse(responseCode = "400", description = "failure to process request")
    })
    @PostMapping(value = "/init", consumes = "application/json", produces = "application/json")
    ResponseEntity<String> initiateDeviceRegistration(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody DeviceRegInitRequest regInitRequest);


    @Operation(summary = "Validate PoW ", description = "Validates the POW computed by the FE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successfully validated device"),
            @ApiResponse(responseCode = "400", description = "Failed to validate device")
    })
    @PostMapping(value = "/validate", consumes = "application/json", produces = "application/json")
    ResponseEntity<String> validateDeviceRegistration(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody DeviceValidateRequest deviceValidateRequest);

}