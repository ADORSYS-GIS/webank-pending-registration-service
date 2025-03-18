package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.repository.PersonalInfoRepository;
import com.adorsys.webank.service.PersonalInfoServiceApi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
class PersonalInfoServiceImpl implements PersonalInfoServiceApi {

    private final PersonalInfoRepository repository;

    public PersonalInfoServiceImpl(PersonalInfoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void savePersonalInfo(PersonalInfoEntity personalInfoEntity) {
        repository.save(personalInfoEntity);
    }

    @Override
    public Optional<PersonalInfoEntity> getPersonalInfoByPublicKey(String publicKeyHash) {
        return repository.findByPublicKeyHash(publicKeyHash);
    }

    @Override
    public List<PersonalInfoEntity> getPersonalInfoByStatus(PersonalInfoStatus status) {
        return repository.findByStatus(status);
    }
}

