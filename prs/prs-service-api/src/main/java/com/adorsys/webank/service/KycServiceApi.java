package com.adorsys.webank.service;
import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.domain.UserDocumentsEntity;
import com.adorsys.webank.dto.KycDocumentRequest;
import com.adorsys.webank.dto.KycEmailRequest;
import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.dto.KycLocationRequest;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface KycServiceApi {
    String sendKycDocument( KycDocumentRequest kycDocumentRequest);
    String sendKycinfo( KycInfoRequest kycInfoRequest);
    String sendKyclocation(JWK devicePub, KycLocationRequest kycLocationRequest);
    String sendKycEmail(JWK devicePub, KycEmailRequest kycEmailRequest);
    Optional<UserDocumentsEntity> getDocuments(String publicKeyHash);
    Optional<PersonalInfoEntity> getPersonalInfoByPublicKey(String publicKeyHash);
    List<PersonalInfoEntity> getPersonalInfoByStatus(PersonalInfoStatus status);
}
