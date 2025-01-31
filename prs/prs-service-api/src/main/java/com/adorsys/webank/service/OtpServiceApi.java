package com.adorsys.webank.service;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.stereotype.Service;

@Service
public interface OtpServiceApi {
    String generateOtp();
    String sendOtp(JWK devicePub, String phoneNumber );
    String computeHash(String input);
    String validateOtp(String phoneNumber, JWK devicePub, String otpInput , String otpHash);
}
