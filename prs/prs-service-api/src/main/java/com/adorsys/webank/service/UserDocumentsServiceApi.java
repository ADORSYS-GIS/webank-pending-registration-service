package com.adorsys.webank.service;

import com.adorsys.webank.domain.UserDocumentsEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface UserDocumentsServiceApi {
    void saveDocuments(String publicKeyHash, MultipartFile frontID, MultipartFile backID,
                       MultipartFile selfieID, MultipartFile taxID) throws IOException;
    Optional<UserDocumentsEntity> getDocuments(String publicKeyHash);
}
