package com.adorsys.webank.repository;

import com.adorsys.webank.domain.UserDocumentsEntity;
import com.adorsys.webank.projection.UserDocumentsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserDocumentsRepository extends JpaRepository<UserDocumentsEntity, String> {
    Optional<UserDocumentsProjection> findByAccountId(String accountId);
}
