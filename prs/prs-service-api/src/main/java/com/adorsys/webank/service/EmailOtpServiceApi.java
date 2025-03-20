package com.adorsys.webank.service;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.stereotype.Service;

@Service
public interface EmailOtpServiceApi {
    String generateOtp();
    String sendEmailOtp(JWK publicKey, String email);
    String validateEmailOtp(String email, JWK publicKey, String otpInput);

    }

