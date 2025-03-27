package com.adorsys.webank;

import com.adorsys.webank.dto.EmailOtpRequest;
import com.adorsys.webank.dto.EmailOtpValidationRequest;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.EmailOtpServiceApi;
import com.nimbusds.jose.jwk.JWK;
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
                return "Invalid or unauthorized JWT.";
            }
        } catch (Exception e) {
            return "Invalid JWT: " + e.getMessage();
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
            JwtValidator.validateAndExtract(jwtToken, email, otpInput);

            if (!certValidator.validateJWT(jwtToken)) {
                return "Invalid or unauthorized JWT.";
            }
        } catch (Exception e) {
            return "Invalid JWT: " + e.getMessage();
        }

        return emailOtpService.validateEmailOtp(
                request.getEmail(),
                request.getAccountId(),
                request.getOtp()
        );
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException(
                    "Authorization header must start with 'Bearer '"
            );
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}
