package com.adorsys.webank.service;

import com.adorsys.webank.dto.response.KycDocumentResponse;
import com.adorsys.webank.dto.response.KycInfoResponse;
import com.adorsys.webank.dto.response.KycLocationResponse;
import com.adorsys.webank.dto.response.KycEmailResponse;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.projection.*;
import org.springframework.stereotype.*;

import java.util.*;

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
