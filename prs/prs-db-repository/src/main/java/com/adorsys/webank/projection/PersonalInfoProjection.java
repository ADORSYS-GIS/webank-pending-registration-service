package com.adorsys.webank.projection;

import com.adorsys.webank.domain.PersonalInfoStatus;
import java.time.LocalDateTime;

public interface PersonalInfoProjection {
    String getAccountId();
    String getDocumentUniqueId();
    String getExpirationDate();
    LocalDateTime getOtpExpirationDateTime();
    String getLocation();
    String getEmail();
    String getEmailOtpHash();
    String getEmailOtpCode();
    PersonalInfoStatus getStatus();
    String getRejectionReason();
} 