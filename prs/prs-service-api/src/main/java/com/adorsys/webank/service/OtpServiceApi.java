package com.adorsys.webank.service;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public interface OtpServiceApi {
    String sendOtp(String phoneNumber);
    boolean validateOtp(String phoneNumber, String otp);
}
