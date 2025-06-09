package com.adorsys.webank.service;
import org.springframework.stereotype.Service;

@Service
public interface OtpServiceApi {
    String generateOtp();
    String sendOtp(String phoneNumber ) ;
    String computeHash(String input);
    String validateOtp(String phoneNumber,  String otpInput ) ;
}


