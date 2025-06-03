package com.adorsys.webank;

import com.adorsys.webank.dto.*;
import com.adorsys.webank.exceptions.JwtValidationException;
import com.adorsys.webank.security.*;
import com.adorsys.webank.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.proc.BadJOSEException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;

@RestController
@Slf4j
public class KycRestServer implements KycRestApi {
    private final KycServiceApi kycServiceApi;
    private final CertValidator certValidator;

    public KycRestServer(KycServiceApi kycServiceApi, CertValidator certValidator) {
        this.kycServiceApi = kycServiceApi;
        this.certValidator = certValidator;
    }

    @Override
    public String sendKycinfo(String authorizationHeader, KycInfoRequest kycInfoRequest) {
        // Extract JWT token from header and validate it
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        log.debug("Processing KYC info request for accountId: {}", kycInfoRequest.getAccountId());
        
        validateJwtAndExtractKey(jwtToken, kycInfoRequest.getIdNumber(), kycInfoRequest.getExpiryDate(), kycInfoRequest.getAccountId());
        
        return kycServiceApi.sendKycInfo(kycInfoRequest.getAccountId(), kycInfoRequest);
    }
    
    /**
     * Validates JWT token and extracts the public key
     * @throws JwtValidationException if validation fails
     */
    private JWK validateJwtAndExtractKey(String jwtToken, String... params) {
        try {
            // Validate the JWT token using the injected CertValidator instance
            if (!certValidator.validateJWT(jwtToken)) {
                log.warn("JWT signature validation failed");
                throw new JwtValidationException("Invalid or unauthorized JWT");
            }
            
            // Only validate and extract the public key after JWT validation passes
            return JwtValidator.validateAndExtract(jwtToken, params);
        } catch (ParseException | JOSEException | BadJOSEException | NoSuchAlgorithmException | JsonProcessingException | JwtValidationException e) {
            // Handle all exceptions in a single catch block
            log.warn("JWT validation failed: {}", e.getMessage());
            
            if (e instanceof JwtValidationException) {
                throw (JwtValidationException) e;  // Cast and throw without creating new exception
            } else {
                throw new JwtValidationException("Invalid JWT: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String sendKyclocation(String authorizationHeader, KycLocationRequest kycLocationRequest) {
        // Extract JWT token from header and validate it
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        log.debug("Processing KYC location request for accountId: {}", kycLocationRequest.getAccountId());
        
        validateJwtAndExtractKey(jwtToken, kycLocationRequest.getLocation(), kycLocationRequest.getAccountId());
        
        return kycServiceApi.sendKycLocation(kycLocationRequest);
    }

    @Override
    public String sendKycEmail(String authorizationHeader, KycEmailRequest kycEmailRequest) {
        // Extract JWT token from header and validate it
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        log.debug("Processing KYC email request");
        
        validateJwtAndExtractKey(jwtToken);
        
        return kycServiceApi.sendKycEmail(kycEmailRequest);
    }

    @Override
    public String sendKycDocument(String authorizationHeader, KycDocumentRequest kycDocumentRequest) {
        // Extract JWT token from header and validate it
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        log.debug("Processing KYC document request for accountId: {}", kycDocumentRequest.getAccountId());
        
        validateJwtAndExtractKey(jwtToken,
                kycDocumentRequest.getFrontId(), kycDocumentRequest.getBackId(),
                kycDocumentRequest.getSelfieId(), kycDocumentRequest.getTaxId(),
                kycDocumentRequest.getAccountId());
        
        return kycServiceApi.sendKycDocument(kycDocumentRequest.getAccountId(), kycDocumentRequest);
    }

    @Override
    public List<UserInfoResponse> getPendingKycRecords(String authorizationHeader) {
        // Extract JWT token from header and validate it
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        log.debug("Processing pending KYC records request");
        
        validateJwtAndExtractKey(jwtToken);
        
        return kycServiceApi.getPendingKycRecords();
    }

    @Override
    public List<UserInfoResponse> findByDocumentUniqueId(String authorizationHeader, String DocumentUniqueId) {
        // Extract JWT token from header and validate it
        String jwtToken = extractJwtFromHeader(authorizationHeader);
        log.debug("Processing document lookup by unique ID: {}", DocumentUniqueId);
        
        validateJwtAndExtractKey(jwtToken, DocumentUniqueId);
        
        return kycServiceApi.findByDocumentUniqueId(DocumentUniqueId);
    }

    /**
     * Extracts JWT token from Authorization header
     * @throws JwtValidationException if header format is invalid
     */
    private String extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new JwtValidationException("Authorization header must start with 'Bearer '");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }
}
