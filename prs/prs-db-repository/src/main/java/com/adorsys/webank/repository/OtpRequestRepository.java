package com.adorsys.webank.repository;
import com.adorsys.webank.domain.OtpRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpRequestRepository extends JpaRepository<com.adorsys.webank.domain.OtpRequest, UUID> {
    Optional<OtpRequest> findByPublicKeyHash(String publicKeyHash);
}