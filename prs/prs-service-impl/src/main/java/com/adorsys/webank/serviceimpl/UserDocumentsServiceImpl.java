package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.UserDocumentsEntity;
import com.adorsys.webank.repository.UserDocumentsRepository;
import com.adorsys.webank.service.UserDocumentsServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;



@Service
class UserDocumentsServiceImpl implements UserDocumentsServiceApi {

    private final UserDocumentsRepository repository;

    public UserDocumentsServiceImpl(UserDocumentsRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveDocuments(String publicKeyHash, MultipartFile frontID, MultipartFile backID,
                              MultipartFile selfieID, MultipartFile taxID) throws IOException {
        UserDocumentsEntity userDocuments = new UserDocumentsEntity();
        userDocuments.setPublicKeyHash(publicKeyHash);
        userDocuments.setFrontID(frontID.getBytes());
        userDocuments.setBackID(backID.getBytes());
        userDocuments.setSelfieID(selfieID.getBytes());
        userDocuments.setTaxID(taxID.getBytes());
        repository.save(userDocuments);
    }

    @Override
    public Optional<UserDocumentsEntity> getDocuments(String publicKeyHash) {
        return repository.findById(publicKeyHash);
    }
}

