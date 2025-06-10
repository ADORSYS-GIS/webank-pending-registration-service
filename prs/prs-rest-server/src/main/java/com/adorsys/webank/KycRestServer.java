package com.adorsys.webank;

import com.adorsys.webank.dto.*;
import com.adorsys.webank.service.*;
import org.slf4j.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class KycRestServer implements KycRestApi {
    private static final Logger log = LoggerFactory.getLogger(KycRestServer.class);
    private final KycServiceApi kycServiceApi;

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendKycinfo(String authorizationHeader, KycInfoRequest kycInfoRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC info request [correlationId={}]", correlationId);
        
        String accountId = kycInfoRequest.getAccountId();
        MDC.put("accountId", maskAccountId(accountId));
        
        try {
            log.debug("Processing KYC info for account [correlationId={}]", correlationId);
            String result = kycServiceApi.sendKycInfo(accountId, kycInfoRequest);
            log.info("KYC info processed successfully [correlationId={}]", correlationId);
            return result;
        } catch (Exception e) {
            log.error("Failed to process KYC info [correlationId={}]", correlationId, e);
            throw e;
        } finally {
            MDC.remove("accountId");
        }
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendKyclocation(String authorizationHeader, KycLocationRequest kycLocationRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC location request [correlationId={}]", correlationId);
        
        String accountId = kycLocationRequest.getAccountId();
        MDC.put("accountId", maskAccountId(accountId));
        
        try {
            log.debug("Processing KYC location for account [correlationId={}]", correlationId);
            String result = kycServiceApi.sendKycLocation(kycLocationRequest);
            log.info("KYC location processed successfully [correlationId={}]", correlationId);
            return result;
        } catch (Exception e) {
            log.error("Failed to process KYC location [correlationId={}]", correlationId, e);
            throw e;
        } finally {
            MDC.remove("accountId");
        }
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendKycEmail(String authorizationHeader, KycEmailRequest kycEmailRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC email request [correlationId={}]", correlationId);
        
        String accountId = kycEmailRequest.getAccountId();
        String email = kycEmailRequest.getEmail();
        
        MDC.put("accountId", maskAccountId(accountId));
        MDC.put("email", maskEmail(email));
        
        try {
            log.debug("Processing KYC email for account [correlationId={}]", correlationId);
            String result = kycServiceApi.sendKycEmail(kycEmailRequest);
            log.info("KYC email processed successfully [correlationId={}]", correlationId);
            return result;
        } catch (Exception e) {
            log.error("Failed to process KYC email [correlationId={}]", correlationId, e);
            throw e;
        } finally {
            MDC.remove("accountId");
            MDC.remove("email");
        }
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendKycDocument(String authorizationHeader, KycDocumentRequest kycDocumentRequest) {
        String correlationId = MDC.get("correlationId");
        log.info("Received KYC document request [correlationId={}]", correlationId);
        
        String accountId = kycDocumentRequest.getAccountId();
        MDC.put("accountId", maskAccountId(accountId));
        
        try {
            log.debug("Processing KYC document for account [correlationId={}]", correlationId);
            String result = kycServiceApi.sendKycDocument(accountId, kycDocumentRequest);
            log.info("KYC document processed successfully [correlationId={}]", correlationId);
            return result;
        } catch (Exception e) {
            log.error("Failed to process KYC document [correlationId={}]", correlationId, e);
            throw e;
        } finally {
            MDC.remove("accountId");
        }
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public List<UserInfoResponse> getPendingKycRecords(String authorizationHeader) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to get pending KYC records [correlationId={}]", correlationId);
        
        try {
            log.debug("Retrieving pending KYC records [correlationId={}]", correlationId);
            List<UserInfoResponse> records = kycServiceApi.getPendingKycRecords();
            log.info("Retrieved {} pending KYC records [correlationId={}]", 
                    records.size(), correlationId);
            return records;
        } catch (Exception e) {
            log.error("Failed to retrieve pending KYC records [correlationId={}]", correlationId, e);
            throw e;
        }
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public List<UserInfoResponse> findByDocumentUniqueId(String authorizationHeader, String documentUniqueId) {
        String correlationId = MDC.get("correlationId");
        log.info("Received request to find KYC by document ID [correlationId={}]", correlationId);
        
        MDC.put("documentId", maskDocumentId(documentUniqueId));
        
        try {
            log.debug("Searching for document with ID [correlationId={}]", correlationId);
            List<UserInfoResponse> records = kycServiceApi.findByDocumentUniqueId(documentUniqueId);
            log.info("Found {} records with document ID [correlationId={}]", 
                    records.size(), correlationId);
            return records;
        } catch (Exception e) {
            log.error("Failed to find KYC by document ID [correlationId={}]", correlationId, e);
            throw e;
        } finally {
            MDC.remove("documentId");
        }
    }
    
    /**
     * Masks an account ID for logging purposes
     */
    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() < 5) {
            return "********";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
    
    /**
     * Masks a document ID for logging purposes
     */
    private String maskDocumentId(String documentId) {
        if (documentId == null || documentId.length() < 5) {
            return "********";
        }
        return documentId.substring(0, 2) + "****" + documentId.substring(documentId.length() - 2);
    }
    
    /**
     * Masks an email address for logging purposes
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return "********";
        }
        return email.substring(0, 1) + "********";
    }
}
