package com.adorsys.webank.service;

import org.springframework.stereotype.Service;

@Service
public interface KycCertServiceApi {
    String getCert(String accountId);
}
