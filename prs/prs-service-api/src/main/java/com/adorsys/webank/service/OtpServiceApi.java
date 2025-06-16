package com.adorsys.webank.service;
import org.springframework.stereotype.Service;
import com.adorsys.webank.dto.response.OtpResponse;
import com.adorsys.webank.dto.response.OtpValidationResponse;

@Service
public interface OtpServiceApi {
    String generateOtp();
    OtpResponse sendOtp(String phoneNumber ) ;
    String computeHash(String input);
    OtpValidationResponse validateOtp(String phoneNumber,  String otpInput ) ;
}


