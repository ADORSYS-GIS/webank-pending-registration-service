package com.adorsys.webank;

import com.adorsys.webank.dto.EmailOtpRequest;
import com.adorsys.webank.dto.EmailOtpValidationRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.EmailOtpServiceApi;
import com.adorsys.error.JwtValidationException;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailOtpRestServer implements EmailOtpRestApi {
    private final EmailOtpServiceApi emailOtpService;
    private final CertValidator certValidator;

    public EmailOtpRestServer(EmailOtpServiceApi emailOtpService , CertValidator certValidator) {
        this.emailOtpService = emailOtpService;
        this.certValidator = certValidator;
    }

    @Override
    public String sendEmailOtp(String authorizationHeader,
                               EmailOtpRequest request) {
        String jwtToken;
        try {
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String email = request.getEmail();
            JwtValidator.validateAndExtract(jwtToken, email, request.getAccountId());

            if (!certValidator.validateJWT(jwtToken)) {
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }
        return emailOtpService.sendEmailOtp(request.getAccountId(), request.getEmail());
    }

    @Override
    public String validateEmailOtp(String authorizationHeader,
                                   EmailOtpValidationRequest request) {
        String jwtToken;
        try {
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String email = request.getEmail();
            String otpInput = request.getOtp();
            String accountId = request.getAccountId();
            JwtValidator.validateAndExtract(jwtToken, email, otpInput, accountId);

            if (!certValidator.validateJWT(jwtToken)) {
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }

        return emailOtpService.validateEmailOtp(
                request.getEmail(),
                request.getOtp(),
                request.getAccountId()
        );
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {
            throw new JwtValidationException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}
