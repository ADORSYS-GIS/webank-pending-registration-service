package com.adorsys.webank.repository;
import com.adorsys.webank.domain.OtpEntity;

import java.util.List;
import java.util.UUID;

import com.adorsys.webank.domain.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpRequestRepository extends JpaRepository<OtpEntity, UUID> {
    Optional<OtpEntity> findByPublicKeyHash(String publicKeyHash);
    List<OtpEntity> findByStatus(OtpStatus status);
    Optional<OtpEntity> findByPhoneNumberAndPublicKeyHash(String phoneNumber, String publicKeyHash);

}