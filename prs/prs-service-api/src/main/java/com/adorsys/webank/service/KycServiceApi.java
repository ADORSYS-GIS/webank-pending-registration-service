package com.adorsys.webank.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.adorsys.webank.dto.KycDocumentRequest;
import com.adorsys.webank.dto.KycEmailRequest;
import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.dto.KycLocationRequest;
import com.adorsys.webank.dto.UserInfoResponse;
import com.adorsys.webank.projection.PersonalInfoProjection;

@Service
public interface KycServiceApi {
    String sendKycDocument(String AccountId, KycDocumentRequest kycDocumentRequest);
    String sendKycInfo(String AccountId, KycInfoRequest kycInfoRequest);
    String sendKycLocation(KycLocationRequest kycLocationRequest);
    String sendKycEmail(KycEmailRequest kycEmailRequest);
    Optional<PersonalInfoProjection> getPersonalInfoAccountId(String accountId);
    List<UserInfoResponse> getPendingKycRecords();
    List<UserInfoResponse> findByDocumentUniqueId(String documentUniqueId);
}
