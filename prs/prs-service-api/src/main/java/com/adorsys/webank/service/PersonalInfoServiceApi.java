package com.adorsys.webank.service;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;

import java.util.List;
import java.util.Optional;

public interface PersonalInfoServiceApi {
    void savePersonalInfo(PersonalInfoEntity personalInfoEntity);
    Optional<PersonalInfoEntity> getPersonalInfoByPublicKey(String publicKeyHash);
    List<PersonalInfoEntity> getPersonalInfoByStatus(PersonalInfoStatus status);
}
