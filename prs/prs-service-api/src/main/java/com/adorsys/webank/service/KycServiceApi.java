package com.adorsys.webank.service;
import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.domain.UserDocumentsEntity;
import com.adorsys.webank.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface KycServiceApi {
    String sendKycDocument( String AccountId, KycDocumentRequest kycDocumentRequest);
    String sendKycinfo( String AccountId, KycInfoRequest kycInfoRequest);
    String sendKyclocation( KycLocationRequest kycLocationRequest);
    String sendKycEmail(KycEmailRequest kycEmailRequest);
    Optional<UserDocumentsEntity> getDocuments(String accountId);
    Optional<PersonalInfoEntity> getPersonalInfoByPublicKey(String accountId);
    List<PersonalInfoEntity> getPersonalInfoByStatus(PersonalInfoStatus status);
    List<UserInfoResponse> findByDocumentUniqueId(String documentUniqueId);

}
