package com.adorsys.webank.projection;

import com.adorsys.webank.domain.UserDocumentsStatus;

public interface UserDocumentsProjection {
    String getAccountId();
    String getFrontID();
    String getBackID();
    String getSelfieID();
    String getTaxID();
    UserDocumentsStatus getStatus();
} 