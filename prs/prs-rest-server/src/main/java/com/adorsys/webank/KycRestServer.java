package com.adorsys.webank;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.security.CertValidator;
import com.adorsys.webank.security.JwtValidator;
import com.adorsys.webank.service.KycServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class KycRestServer implements KycRestApi {
    private static final Logger log = LoggerFactory.getLogger(KycRestServer.class);
    private final KycServiceApi kycServiceApi;
    private final CertValidator certValidator;

    public KycRestServer(KycServiceApi kycServiceApi, CertValidator certValidator) {
        this.kycServiceApi = kycServiceApi;
        this.certValidator = certValidator;
    }


    @Override
    public String sendKycinfo(String authorizationHeader, KycInfoRequest kycInfoRequest) {
        String jwtToken;

        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            JwtValidator.validateAndExtract(jwtToken, kycInfoRequest.getIdNumber(),kycInfoRequest.getExpiryDate(), kycInfoRequest.getAccountId());
            log.info("Successfully validated sendinfo");

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {

                return "Invalid or unauthorized JWT.";
            }
        } catch (Exception e) {
            return "Invalid JWT: " + e.getMessage();
        }

        String AccountId = kycInfoRequest.getAccountId();

        return kycServiceApi.sendKycinfo( AccountId, kycInfoRequest);
    }

    @Override
    public String sendKyclocation(String authorizationHeader, KycLocationRequest kycLocationRequest) {
        String jwtToken;

        String location = kycLocationRequest.getLocation();
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            JwtValidator.validateAndExtract(jwtToken, location, kycLocationRequest.getAccountId());

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {

                return "Invalid or unauthorized JWT.";
            }
        } catch (Exception e) {
            return "Invalid JWT: " + e.getMessage();
        }

        return kycServiceApi.sendKyclocation( kycLocationRequest);
    }

    @Override
    public String sendKycEmail(String authorizationHeader, KycEmailRequest kycEmailRequest) {
        String jwtToken;

        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            JwtValidator.validateAndExtract(jwtToken);

            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {

                return "Invalid or unauthorized JWT.";
            }
        } catch (Exception e) {
            return "Invalid JWT: " + e.getMessage();
        }

        return kycServiceApi.sendKycEmail( kycEmailRequest);
    }

    @Override
    public List<PersonalInfoEntity>  getPersonalInfoByStatus(String authorizationHeader) {
        String jwtToken;
        try {
            // Extract the JWT token from the Authorization header
            jwtToken = extractJwtFromHeader(authorizationHeader);
            JwtValidator.validateAndExtract(jwtToken);
            log.info("Success");

        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred");
        }
        return kycServiceApi.getPersonalInfoByStatus(PersonalInfoStatus.valueOf("PENDING"));
    }

    @Override
    public List<UserInfoResponse> findByDocumentUniqueId(String authorizationHeader, String DocumentUniqueId) {
//        String jwtToken;
        try {
            // Extract the JWT token from the Authorization header
//            jwtToken = extractJwtFromHeader(authorizationHeader);
//            JwtValidator.validateAndExtract(jwtToken);
            log.info("Success");

        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred");
        }
        return kycServiceApi.findByDocumentUniqueId(DocumentUniqueId);

    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }


}