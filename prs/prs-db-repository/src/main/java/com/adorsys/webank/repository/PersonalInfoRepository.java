package com.adorsys.webank.repository;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import com.adorsys.webank.projection.PersonalInfoProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalInfoRepository extends JpaRepository<PersonalInfoEntity, String> {
    Optional<PersonalInfoProjection> findByAccountId(String accountId);
    List<PersonalInfoProjection> findByStatus(PersonalInfoStatus status);
    List<PersonalInfoProjection> findByDocumentUniqueId(String documentUniqueId);
}
