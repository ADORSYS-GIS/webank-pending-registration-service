package com.adorsys.webank.repository;

import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.PersonalInfoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonalInfoRepository extends JpaRepository<PersonalInfoEntity, UUID> {
    Optional<PersonalInfoEntity> findByAccountId(String accountId);
    List<PersonalInfoEntity> findByStatus(PersonalInfoStatus status);
    List<PersonalInfoEntity> findByDocumentUniqueId(String documentUniqueId);

}
