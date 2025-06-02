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

        log.info("Sending KYC info for accountId: {}", kycInfoRequest.getAccountId());
        return kycServiceApi.sendKycInfo(kycInfoRequest.getAccountId(), kycInfoRequest);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendKyclocation(String authorizationHeader, KycLocationRequest kycLocationRequest) {
        log.info("Sending KYC location for accountId: {}", kycLocationRequest.getAccountId());
        return kycServiceApi.sendKycLocation(kycLocationRequest);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendKycEmail(String authorizationHeader, KycEmailRequest kycEmailRequest) {
        log.info("Sending KYC email for accountId: {}", kycEmailRequest.getAccountId());
        return kycServiceApi.sendKycEmail(kycEmailRequest);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public String sendKycDocument(String authorizationHeader, KycDocumentRequest kycDocumentRequest) {
        log.info("Sending KYC document for accountId: {}", kycDocumentRequest.getAccountId());
        return kycServiceApi.sendKycDocument(kycDocumentRequest.getAccountId(), kycDocumentRequest);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public List<UserInfoResponse> getPendingKycRecords(String authorizationHeader) {
        log.info("Agent requesting pending KYC records");
        return kycServiceApi.getPendingKycRecords();
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ACCOUNT_CERTIFIED') and isAuthenticated()")
    public List<UserInfoResponse> findByDocumentUniqueId(String authorizationHeader, String DocumentUniqueId) {
        log.info("Agent searching for document unique ID: {}", DocumentUniqueId);
        return kycServiceApi.findByDocumentUniqueId(DocumentUniqueId);
    }


}
