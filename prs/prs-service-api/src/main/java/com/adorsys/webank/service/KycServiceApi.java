package com.adorsys.webank.service;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.dto.*;
import com.adorsys.webank.projection.*;
import org.springframework.stereotype.*;

import java.util.*;

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
