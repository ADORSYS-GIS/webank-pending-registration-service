package com.adorsys.webank.service;

import com.nimbusds.jose.jwk.JWK;
import org.springframework.stereotype.Service;

@Service
public interface KycCertServiceApi {
    String getCert(String accountId);
}
