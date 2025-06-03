package com.adorsys.webank.repository;
import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.projection.OtpProjection;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;


@Repository
public interface OtpRequestRepository extends JpaRepository<OtpEntity, UUID> {
    Optional<OtpProjection> findByPublicKeyHash(String publicKeyHash);
    
    // Added method to get the full entity for update operations
    @Query("SELECT o FROM OtpEntity o WHERE o.publicKeyHash = :publicKeyHash")
    Optional<OtpEntity> findEntityByPublicKeyHash(@Param("publicKeyHash") String publicKeyHash);
    
    List<OtpProjection> findByStatus(OtpStatus status);

    @Modifying
    @Query("UPDATE OtpEntity o SET " +
            "o.otpCode = :otpCode, " +
            "o.status = :status, " +
            "o.updatedAt = :updatedAt " +
            "WHERE o.publicKeyHash = :publicKeyHash")
    int updateOtpByPublicKeyHash(
            @Param("publicKeyHash") String publicKeyHash,
            @Param("otpCode") String otpCode,
            @Param("status") OtpStatus status,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}