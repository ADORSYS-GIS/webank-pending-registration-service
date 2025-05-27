package com.adorsys.webank;

import com.adorsys.webank.dto.*;
import com.adorsys.webank.security.*;
import com.adorsys.webank.service.*;
import com.adorsys.error.JwtValidationException;
import org.slf4j.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from sending info is: {}, for accountId: {}", jwtToken, kycInfoRequest.getAccountId());
            JwtValidator.validateAndExtract(jwtToken, kycInfoRequest.getIdNumber(), kycInfoRequest.getExpiryDate(), kycInfoRequest.getAccountId());
            if (!certValidator.validateJWT(jwtToken)) {
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }

            return kycServiceApi.sendKycInfo(kycInfoRequest.getAccountId(), kycInfoRequest);
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }
    }

    @Override
    public String sendKyclocation(String authorizationHeader, KycLocationRequest kycLocationRequest) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from sending location is: {}, for accountId: {}", jwtToken, kycLocationRequest.getAccountId());
            JwtValidator.validateAndExtract(jwtToken, kycLocationRequest.getLocation(), kycLocationRequest.getAccountId());
            if (!certValidator.validateJWT(jwtToken)) {
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }

            return kycServiceApi.sendKycLocation(kycLocationRequest);
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }
    }

    @Override
    public String sendKycEmail(String authorizationHeader, KycEmailRequest kycEmailRequest) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from user sending email is  : {}", jwtToken);
            JwtValidator.validateAndExtract(jwtToken);
            if (!certValidator.validateJWT(jwtToken)) {
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }

            return kycServiceApi.sendKycEmail(kycEmailRequest);
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }
    }

    @Override
    public String sendKycDocument(String authorizationHeader, KycDocumentRequest kycDocumentRequest) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from sending document is: {}, for accountId: {}", jwtToken, kycDocumentRequest.getAccountId());
            JwtValidator.validateAndExtract(jwtToken,
                    kycDocumentRequest.getFrontId(), kycDocumentRequest.getBackId(),
                    kycDocumentRequest.getSelfieId(), kycDocumentRequest.getTaxId(),
                    kycDocumentRequest.getAccountId());

            if (!certValidator.validateJWT(jwtToken)) {
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }

            return kycServiceApi.sendKycDocument(kycDocumentRequest.getAccountId(), kycDocumentRequest);
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }
    }

    @Override
    public List<UserInfoResponse> getPendingKycRecords(String authorizationHeader) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from agent doing verification is : {}", jwtToken);
            JwtValidator.validateAndExtract(jwtToken);

            if (!certValidator.validateJWT(jwtToken)) {
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }

            return kycServiceApi.getPendingKycRecords();
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }
    }

    @Override
    public List<UserInfoResponse> findByDocumentUniqueId(String authorizationHeader, String DocumentUniqueId) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from agent doing recovery is : {}", jwtToken);
            JwtValidator.validateAndExtract(jwtToken, DocumentUniqueId);

            if (!certValidator.validateJWT(jwtToken)) {
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }

            return kycServiceApi.findByDocumentUniqueId(DocumentUniqueId);
        } catch (Exception e) {
            throw new JwtValidationException("JWT validation failed: " + e.getMessage());
        }
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new JwtValidationException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}
