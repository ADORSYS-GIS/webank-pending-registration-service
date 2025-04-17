package com.adorsys.webank.repository;

import com.adorsys.webank.domain.UserDocumentsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserDocumentsRepository extends JpaRepository<UserDocumentsEntity, String> {
    Optional<UserDocumentsEntity> findByAccountId(String accountId);
}
