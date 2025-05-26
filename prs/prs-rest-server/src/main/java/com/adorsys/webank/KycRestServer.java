package com.adorsys.webank;

import com.adorsys.webank.dto.*;
import com.adorsys.webank.dto.response.KycResponse;
import com.adorsys.webank.security.*;
import com.adorsys.webank.service.*;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    public ResponseEntity<KycResponse> sendKycinfo(String authorizationHeader, KycInfoRequest kycInfoRequest) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from sending info is: {}, for accountId: {}", jwtToken, kycInfoRequest.getAccountId());
            JwtValidator.validateAndExtract(jwtToken, kycInfoRequest.getIdNumber(), kycInfoRequest.getExpiryDate(), kycInfoRequest.getAccountId());
            if (!certValidator.validateJWT(jwtToken)) {
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            String result = kycServiceApi.sendKycInfo(kycInfoRequest.getAccountId(), kycInfoRequest);
            KycResponse response = new KycResponse();
            response.setKycId("kyc_info_" + System.currentTimeMillis());
            response.setStatus(KycResponse.KycStatus.PENDING);
            response.setSubmittedAt(LocalDateTime.now());
            response.setMessage(result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<KycResponse> sendKyclocation(String authorizationHeader, KycLocationRequest kycLocationRequest) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from sending location is: {}, for accountId: {}", jwtToken, kycLocationRequest.getAccountId());
            JwtValidator.validateAndExtract(jwtToken, kycLocationRequest.getLocation(), kycLocationRequest.getAccountId());
            if (!certValidator.validateJWT(jwtToken)) {
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            String result = kycServiceApi.sendKycLocation(kycLocationRequest);
            KycResponse response = new KycResponse();
            response.setKycId("kyc_location_" + System.currentTimeMillis());
            response.setStatus(KycResponse.KycStatus.PENDING);
            response.setSubmittedAt(LocalDateTime.now());
            response.setMessage(result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<KycResponse> sendKycEmail(String authorizationHeader, KycEmailRequest kycEmailRequest) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from user sending email is  : {}", jwtToken);
            JwtValidator.validateAndExtract(jwtToken);
            if (!certValidator.validateJWT(jwtToken)) {
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            String result = kycServiceApi.sendKycEmail(kycEmailRequest);
            KycResponse response = new KycResponse();
            response.setKycId("kyc_email_" + System.currentTimeMillis());
            response.setStatus(KycResponse.KycStatus.PENDING);
            response.setSubmittedAt(LocalDateTime.now());
            response.setMessage(result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<KycResponse> sendKycDocument(String authorizationHeader, KycDocumentRequest kycDocumentRequest) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from sending document is: {}, for accountId: {}", jwtToken, kycDocumentRequest.getAccountId());
            JwtValidator.validateAndExtract(jwtToken,
                    kycDocumentRequest.getFrontId(), kycDocumentRequest.getBackId(),
                    kycDocumentRequest.getSelfieId(), kycDocumentRequest.getTaxId(),
                    kycDocumentRequest.getAccountId());

            if (!certValidator.validateJWT(jwtToken)) {
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            String result = kycServiceApi.sendKycDocument(kycDocumentRequest.getAccountId(), kycDocumentRequest);
            KycResponse response = new KycResponse();
            response.setKycId("kyc_doc_" + System.currentTimeMillis());
            response.setStatus(KycResponse.KycStatus.PENDING);
            response.setSubmittedAt(LocalDateTime.now());
            response.setMessage(result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<List<UserInfoResponse>> getPendingKycRecords(String authorizationHeader) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from agent doing verification is : {}", jwtToken);
            JwtValidator.validateAndExtract(jwtToken);

            if (!certValidator.validateJWT(jwtToken)) {
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            List<UserInfoResponse> results = kycServiceApi.getPendingKycRecords();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            throw new IllegalArgumentException("JWT validation failed: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<List<UserInfoResponse>> findByDocumentUniqueId(String authorizationHeader, String DocumentUniqueId) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from agent doing recovery is : {}", jwtToken);
            JwtValidator.validateAndExtract(jwtToken, DocumentUniqueId);

            if (!certValidator.validateJWT(jwtToken)) {
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            List<UserInfoResponse> results = kycServiceApi.findByDocumentUniqueId(DocumentUniqueId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            throw new IllegalArgumentException("JWT validation failed: " + e.getMessage());
        }
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
    
    private KycResponse createErrorResponse(String message) {
        KycResponse response = new KycResponse();
        response.setKycId("error_" + System.currentTimeMillis());
        response.setStatus(KycResponse.KycStatus.REJECTED);
        response.setSubmittedAt(LocalDateTime.now());
        response.setMessage("Error: " + message);
        return response;
    }
}
