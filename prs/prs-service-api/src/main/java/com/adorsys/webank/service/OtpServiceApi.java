package com.adorsys.webank.service;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public interface OtpServiceApi {
    String generateOtp();
    String sendOtp(String phoneNumber, String publicKey);
    boolean validateOtp(String phoneNumber, String otp);
}
