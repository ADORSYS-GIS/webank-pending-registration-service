package com.adorsys.webank.service;
import org.springframework.stereotype.Service;
import com.adorsys.webank.dto.response.EmailResponse;
import com.adorsys.webank.dto.response.EmailValidationResponse;

@Service
public interface EmailOtpServiceApi {
    String generateOtp();
    EmailResponse sendEmailOtp(String accountId, String email);
    EmailValidationResponse validateEmailOtp(String email, String otpInput, String accountId);
}
