package com.adorsys.webank.service;
import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.dto.KycEmailRequest;
import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.dto.KycLocationRequest;
import org.springframework.stereotype.Service;
import com.adorsys.webank.dto.UserInfoResponse;

import java.util.List;
import java.util.Optional;

@Service
public interface KycServiceApi {
    String sendKycinfo( String AccountId, KycInfoRequest kycInfoRequest);
    String sendKyclocation( KycLocationRequest kycLocationRequest);
    String sendKycEmail(KycEmailRequest kycEmailRequest);
    Optional<PersonalInfoEntity> getPersonalInfoByPublicKey(String accountId);
    List<PersonalInfoEntity> getPersonalInfoByStatus(PersonalInfoStatus status);
    List<UserInfoResponse> findByDocumentUniqueId(String documentUniqueId);
}
