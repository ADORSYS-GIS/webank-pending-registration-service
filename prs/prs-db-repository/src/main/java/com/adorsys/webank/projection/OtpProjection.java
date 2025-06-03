package com.adorsys.webank.projection;

import com.adorsys.webank.domain.OtpStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public interface OtpProjection {
    UUID getId();
    String getPhoneNumber();
    String getPublicKeyHash();
    String getOtpHash();
    String getOtpCode();
    OtpStatus getStatus();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
} 