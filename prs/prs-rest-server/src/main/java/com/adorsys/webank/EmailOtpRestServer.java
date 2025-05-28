package com.adorsys.webank;

import com.adorsys.webank.dto.EmailOtpRequest;
import com.adorsys.webank.dto.EmailOtpValidationRequest;
import com.adorsys.webank.dto.response.EmailResponse;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.EmailOtpServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class EmailOtpRestServer implements EmailOtpRestApi {
    private final EmailOtpServiceApi emailOtpService;
    private final CertValidator certValidator;

    public EmailOtpRestServer(EmailOtpServiceApi emailOtpService , CertValidator certValidator) {
        this.emailOtpService = emailOtpService;
        this.certValidator = certValidator;

    }

    @Override
    public ResponseEntity<EmailResponse> sendEmailOtp(String authorizationHeader,
                               EmailOtpRequest request) {
        String jwtToken;
        try {
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String email = request.getEmail();
            JwtValidator.validateAndExtract(jwtToken, email, request.getAccountId());

            if (!certValidator.validateJWT(jwtToken)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid or unauthorized JWT."));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid JWT: " + e.getMessage()));
        }
        
        String result = emailOtpService.sendEmailOtp(request.getAccountId(), request.getEmail());
        
        EmailResponse response = new EmailResponse();
        response.setStatus(EmailResponse.EmailStatus.SUCCESS);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(result);
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<EmailResponse> validateEmailOtp(String authorizationHeader,
                                   EmailOtpValidationRequest request) {
        String jwtToken;
        try {
            jwtToken = extractJwtFromHeader(authorizationHeader);
            String email = request.getEmail();
            String otpInput = request.getOtp();
            String accountId = request.getAccountId();
            JwtValidator.validateAndExtract(jwtToken, email, otpInput, accountId);


            if (!certValidator.validateJWT(jwtToken)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid or unauthorized JWT."));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid JWT: " + e.getMessage()));
        }

        String result = emailOtpService.validateEmailOtp(
                request.getEmail(),
                request.getOtp(),
                request.getAccountId()
        );
        
        EmailResponse response = new EmailResponse();
        response.setStatus(EmailResponse.EmailStatus.SUCCESS);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(result);
        
        return ResponseEntity.ok(response);
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
    
    private EmailResponse createErrorResponse(String message) {
        EmailResponse response = new EmailResponse();
        response.setStatus(EmailResponse.EmailStatus.FAILED);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("Error: " + message);
        return response;
    }
}
