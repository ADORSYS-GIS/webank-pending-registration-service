package com.adorsys.webank.service;

import com.adorsys.webank.domain.*;
import com.adorsys.webank.dto.*;
import org.springframework.stereotype.*;

import java.util.*;

@Service
public interface KycServiceApi {
    String sendKycDocument( String AccountId, KycDocumentRequest kycDocumentRequest);
    String sendKycinfo( String AccountId, KycInfoRequest kycInfoRequest);
    String sendKyclocation( KycLocationRequest kycLocationRequest);
    String sendKycEmail(KycEmailRequest kycEmailRequest);
    Optional<PersonalInfoEntity> getPersonalInfoAccountId(String accountId);
    List<UserInfoResponse> getPendingKycRecords();
    List<UserInfoResponse> findByDocumentUniqueId(String documentUniqueId);
}
