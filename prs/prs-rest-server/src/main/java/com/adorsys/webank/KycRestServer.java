package com.adorsys.webank;

import com.adorsys.webank.dto.*;
import com.adorsys.webank.dto.response.*;
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
    public ResponseEntity<KycInfoResponse> sendKycinfo(String authorizationHeader, KycInfoRequest kycInfoRequest) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from sending info is: {}, for accountId: {}", jwtToken, kycInfoRequest.getAccountId());
            JwtValidator.validateAndExtract(jwtToken, kycInfoRequest.getIdNumber(), kycInfoRequest.getExpiryDate(), kycInfoRequest.getAccountId());
            if (!certValidator.validateJWT(jwtToken)) {
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            KycInfoResponse response = kycServiceApi.sendKycInfo(kycInfoRequest.getAccountId(), kycInfoRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing KYC info request: {}", e.getMessage());
            KycInfoResponse errorResponse = createInfoErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @Override
    public ResponseEntity<KycLocationResponse> sendKyclocation(String authorizationHeader, KycLocationRequest kycLocationRequest) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from sending location is: {}, for accountId: {}", jwtToken, kycLocationRequest.getAccountId());
            JwtValidator.validateAndExtract(jwtToken, kycLocationRequest.getLocation(), kycLocationRequest.getAccountId());
            if (!certValidator.validateJWT(jwtToken)) {
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            KycLocationResponse response = kycServiceApi.sendKycLocation(kycLocationRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing KYC location request: {}", e.getMessage());
            KycLocationResponse errorResponse = createLocationErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @Override
    public ResponseEntity<KycEmailResponse> sendKycEmail(String authorizationHeader, KycEmailRequest kycEmailRequest) {
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.info("jwtToken from user sending email is: {}", jwtToken);
            JwtValidator.validateAndExtract(jwtToken);
            if (!certValidator.validateJWT(jwtToken)) {
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            KycEmailResponse response = kycServiceApi.sendKycEmail(kycEmailRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing KYC email request: {}", e.getMessage());
            KycEmailResponse errorResponse = createEmailErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @Override
    public ResponseEntity<KycDocumentResponse> sendKycDocument(String authorizationHeader, KycDocumentRequest kycDocumentRequest) {
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

            KycDocumentResponse response = kycServiceApi.sendKycDocument(kycDocumentRequest.getAccountId(), kycDocumentRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing KYC document request: {}", e.getMessage());
            KycDocumentResponse errorResponse = createDocumentErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
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
            log.error("Error retrieving pending KYC records: {}", e.getMessage());
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
            log.error("Error finding KYC records by document ID: {}", e.getMessage());
            throw new IllegalArgumentException("JWT validation failed: " + e.getMessage());
        }
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
    
    private KycDocumentResponse createDocumentErrorResponse(String message) {
        KycDocumentResponse response = new KycDocumentResponse();
        response.setKycId("error_doc_" + System.currentTimeMillis());
        response.setStatus(KycResponse.KycStatus.REJECTED);
        response.setSubmittedAt(LocalDateTime.now());
        response.setMessage("Error: " + message);
        return response;
    }
    
    private KycInfoResponse createInfoErrorResponse(String message) {
        KycInfoResponse response = new KycInfoResponse();
        response.setKycId("error_info_" + System.currentTimeMillis());
        response.setStatus(KycResponse.KycStatus.REJECTED);
        response.setSubmittedAt(LocalDateTime.now());
        response.setMessage("Error: " + message);
        return response;
    }
    
    private KycLocationResponse createLocationErrorResponse(String message) {
        KycLocationResponse response = new KycLocationResponse();
        response.setKycId("error_loc_" + System.currentTimeMillis());
        response.setStatus(KycResponse.KycStatus.REJECTED);
        response.setSubmittedAt(LocalDateTime.now());
        response.setMessage("Error: " + message);
        return response;
    }
    
    private KycEmailResponse createEmailErrorResponse(String message) {
        KycEmailResponse response = new KycEmailResponse();
        response.setKycId("error_email_" + System.currentTimeMillis());
        response.setStatus(KycResponse.KycStatus.REJECTED);
        response.setSubmittedAt(LocalDateTime.now());
        response.setMessage("Error: " + message);
        return response;
    }
}
