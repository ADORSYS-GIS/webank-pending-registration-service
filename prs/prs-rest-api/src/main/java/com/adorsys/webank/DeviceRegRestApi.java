package com.adorsys.webank;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
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
    String initiateDeviceRegistration(@RequestHeader  ("Authorization") String jwtToken );

}
