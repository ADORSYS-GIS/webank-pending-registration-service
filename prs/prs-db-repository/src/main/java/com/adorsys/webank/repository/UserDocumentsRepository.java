package com.adorsys.webank.repository;

import com.adorsys.webank.domain.UserDocumentsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDocumentsRepository extends JpaRepository<UserDocumentsEntity, String> {
}

