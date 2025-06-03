package com.adorsys.webank;

import com.adorsys.webank.dto.*;
import com.adorsys.webank.security.*;
import com.adorsys.webank.service.*;
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
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC info request [correlationId={}]", correlationId);
        
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.debug("Validating JWT token for KYC info [correlationId={}]", correlationId);
            
            String accountId = kycInfoRequest.getAccountId();
            MDC.put("accountId", maskAccountId(accountId));
            
            JwtValidator.validateAndExtract(jwtToken, kycInfoRequest.getIdNumber(), kycInfoRequest.getExpiryDate(), accountId);
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Invalid JWT token for KYC info [correlationId={}]", correlationId);
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            log.debug("JWT validation successful, processing KYC info [correlationId={}]", correlationId);
            String result = kycServiceApi.sendKycInfo(accountId, kycInfoRequest);
            log.info("KYC info processed successfully [correlationId={}]", correlationId);
            return result;
        } catch (Exception e) {
            log.error("Failed to process KYC info [correlationId={}]", correlationId, e);
            return "Invalid JWT: " + e.getMessage();
        } finally {
            MDC.remove("accountId");
        }
    }

    @Override
    public String sendKyclocation(String authorizationHeader, KycLocationRequest kycLocationRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC location request [correlationId={}]", correlationId);
        
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            String accountId = kycLocationRequest.getAccountId();
            
            MDC.put("accountId", maskAccountId(accountId));
            log.debug("Validating JWT token for KYC location [correlationId={}]", correlationId);
            
            JwtValidator.validateAndExtract(jwtToken, kycLocationRequest.getLocation(), accountId);
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Invalid JWT token for KYC location [correlationId={}]", correlationId);
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            log.debug("JWT validation successful, processing KYC location [correlationId={}]", correlationId);
            String result = kycServiceApi.sendKycLocation(kycLocationRequest);
            log.info("KYC location processed successfully [correlationId={}]", correlationId);
            return result;
        } catch (Exception e) {
            log.error("Failed to process KYC location [correlationId={}]", correlationId, e);
            return "Invalid JWT: " + e.getMessage();
        } finally {
            MDC.remove("accountId");
        }
    }

    @Override
    public String sendKycEmail(String authorizationHeader, KycEmailRequest kycEmailRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC email request [correlationId={}]", correlationId);
        
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            String accountId = kycEmailRequest.getAccountId();
            String email = kycEmailRequest.getEmail();
            
            MDC.put("accountId", maskAccountId(accountId));
            MDC.put("email", maskEmail(email));
            
            log.debug("Validating JWT token for KYC email [correlationId={}]", correlationId);
            JwtValidator.validateAndExtract(jwtToken);
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Invalid JWT token for KYC email [correlationId={}]", correlationId);
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            log.debug("JWT validation successful, processing KYC email [correlationId={}]", correlationId);
            String result = kycServiceApi.sendKycEmail(kycEmailRequest);
            log.info("KYC email processed successfully [correlationId={}]", correlationId);
            return result;
        } catch (Exception e) {
            log.error("Failed to process KYC email [correlationId={}]", correlationId, e);
            return "Invalid JWT: " + e.getMessage();
        } finally {
            MDC.remove("accountId");
            MDC.remove("email");
        }
    }

    @Override
    public String sendKycDocument(String authorizationHeader, KycDocumentRequest kycDocumentRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC document request [correlationId={}]", correlationId);
        
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            String accountId = kycDocumentRequest.getAccountId();
            
            MDC.put("accountId", maskAccountId(accountId));
            log.debug("Validating JWT token for KYC document [correlationId={}]", correlationId);
            
            JwtValidator.validateAndExtract(jwtToken,
                    kycDocumentRequest.getFrontId(), kycDocumentRequest.getBackId(),
                    kycDocumentRequest.getSelfieId(), kycDocumentRequest.getTaxId(),
                    accountId);

            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Invalid JWT token for KYC document [correlationId={}]", correlationId);
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            log.debug("JWT validation successful, processing KYC document [correlationId={}]", correlationId);
            String result = kycServiceApi.sendKycDocument(accountId, kycDocumentRequest);
            log.info("KYC document processed successfully [correlationId={}]", correlationId);
            return result;
        } catch (Exception e) {
            log.error("Failed to process KYC document [correlationId={}]", correlationId, e);
            return "Invalid JWT: " + e.getMessage();
        } finally {
            MDC.remove("accountId");
        }
    }

    @Override
    public List<UserInfoResponse> getPendingKycRecords(String authorizationHeader) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to get pending KYC records [correlationId={}]", correlationId);
        
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            log.debug("Validating JWT token for pending KYC records [correlationId={}]", correlationId);
            
            JwtValidator.validateAndExtract(jwtToken);

            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Invalid JWT token for pending KYC records [correlationId={}]", correlationId);
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            log.debug("JWT validation successful, retrieving pending KYC records [correlationId={}]", correlationId);
            List<UserInfoResponse> records = kycServiceApi.getPendingKycRecords();
            log.info("Retrieved {} pending KYC records [correlationId={}]", 
                    records.size(), correlationId);
            return records;
        } catch (Exception e) {
            log.error("Failed to retrieve pending KYC records [correlationId={}]", correlationId, e);
            throw new IllegalArgumentException("JWT validation failed: " + e.getMessage());
        }
    }

    @Override
    public List<UserInfoResponse> findByDocumentUniqueId(String authorizationHeader, String DocumentUniqueId) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to find KYC by document ID [correlationId={}]", correlationId);
        
        try {
            String jwtToken = extractJwtFromHeader(authorizationHeader);
            
            MDC.put("documentId", maskDocumentId(DocumentUniqueId));
            log.debug("Validating JWT token for document search [correlationId={}]", correlationId);
            
            JwtValidator.validateAndExtract(jwtToken, DocumentUniqueId);

            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("Invalid JWT token for document search [correlationId={}]", correlationId);
                throw new SecurityException("Invalid or unauthorized JWT.");
            }

            log.debug("JWT validation successful, searching by document ID [correlationId={}]", correlationId);
            List<UserInfoResponse> records = kycServiceApi.findByDocumentUniqueId(DocumentUniqueId);
            log.info("Found {} records by document ID [correlationId={}]", 
                    records.size(), correlationId);
            return records;
        } catch (Exception e) {
            log.error("Failed to search by document ID [correlationId={}]", correlationId, e);
            throw new IllegalArgumentException("JWT validation failed: " + e.getMessage());
        } finally {
            MDC.remove("documentId");
        }
    }

    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Invalid authorization header format");
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
    
    /**
     * Masks an account ID for logging purposes
     * Shows only first 2 and last 2 characters
     */
    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() < 5) {
            return "********";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
    
    /**
     * Masks a document ID for logging purposes
     * Shows only first 2 and last 2 characters
     */
    private String maskDocumentId(String documentId) {
        if (documentId == null || documentId.length() < 5) {
            return "********";
        }
        return documentId.substring(0, 2) + "****" + documentId.substring(documentId.length() - 2);
    }
    
    /**
     * Masks an email address for logging purposes
     * Shows only first character and domain
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "********";
        }
        
        if (email.contains("@")) {
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                String firstChar = email.substring(0, 1);
                String domain = email.substring(atIndex);
                return firstChar + "****" + domain;
            }
        }
        
        return email.substring(0, 1) + "********";
    }
}
