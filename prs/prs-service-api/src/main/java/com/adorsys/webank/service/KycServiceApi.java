package com.adorsys.webank.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.adorsys.webank.dto.KycDocumentRequest;
import com.adorsys.webank.dto.KycEmailRequest;
import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.dto.KycLocationRequest;
import com.adorsys.webank.dto.UserInfoResponse;
import com.adorsys.webank.dto.response.KycDocumentResponse;
import com.adorsys.webank.dto.response.KycEmailResponse;
import com.adorsys.webank.dto.response.KycInfoResponse;
import com.adorsys.webank.dto.response.KycLocationResponse;
import com.adorsys.webank.projection.PersonalInfoProjection;

@Service
public interface KycServiceApi {
    KycDocumentResponse sendKycDocument(String accountId, KycDocumentRequest kycDocumentRequest);
    KycInfoResponse sendKycInfo(String accountId, KycInfoRequest kycInfoRequest);
    KycLocationResponse sendKycLocation(KycLocationRequest kycLocationRequest);
    KycEmailResponse sendKycEmail(KycEmailRequest kycEmailRequest);
    Optional<PersonalInfoProjection> getPersonalInfoAccountId(String accountId);
    List<UserInfoResponse> getPendingKycRecords();
    List<UserInfoResponse> findByDocumentUniqueId(String documentUniqueId);
}
