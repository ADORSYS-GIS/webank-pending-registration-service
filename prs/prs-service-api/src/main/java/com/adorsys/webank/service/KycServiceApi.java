package com.adorsys.webank.service;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.dto.response.*;
import org.springframework.stereotype.*;

import java.util.*;

@Service
public interface KycServiceApi {
    KycDocumentResponse sendKycDocument(String AccountId, KycDocumentRequest kycDocumentRequest);
    KycInfoResponse sendKycInfo(String AccountId, KycInfoRequest kycInfoRequest);
    KycLocationResponse sendKycLocation(KycLocationRequest kycLocationRequest);
    KycEmailResponse sendKycEmail(KycEmailRequest kycEmailRequest);
    Optional<PersonalInfoEntity> getPersonalInfoAccountId(String accountId);
    List<UserInfoResponse> getPendingKycRecords();
    List<UserInfoResponse> findByDocumentUniqueId(String documentUniqueId);
}
