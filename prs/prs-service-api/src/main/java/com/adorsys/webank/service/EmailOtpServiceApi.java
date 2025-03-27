package com.adorsys.webank.service;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.stereotype.Service;

@Service
public interface EmailOtpServiceApi {
    String generateOtp();
    String sendEmailOtp(String accountId, String email);
    String validateEmailOtp(String email, String accountId, String otpInput);

    }

