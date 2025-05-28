package com.adorsys.webank;

import com.adorsys.webank.dto.EmailOtpRequest;
import com.adorsys.webank.dto.EmailOtpValidationRequest;
import com.adorsys.webank.service.EmailOtpServiceApi;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class EmailOtpRestServer implements EmailOtpRestApi {
    private final EmailOtpServiceApi emailOtpService;

    public EmailOtpRestServer(EmailOtpServiceApi emailOtpService) {
        this.emailOtpService = emailOtpService;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendEmailOtp(String authorizationHeader,
                               EmailOtpRequest request) {

        return emailOtpService.sendEmailOtp(request.getAccountId(), request.getEmail());
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String validateEmailOtp(String authorizationHeader,
                                   EmailOtpValidationRequest request) {

        return emailOtpService.validateEmailOtp(
                request.getEmail(),
                request.getOtp(),
                request.getAccountId()
        );
    }

}
