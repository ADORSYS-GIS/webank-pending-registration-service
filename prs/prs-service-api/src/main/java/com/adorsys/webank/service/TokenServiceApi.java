package com.adorsys.webank.service;

import com.adorsys.webank.dto.KycEmailRequest;
import com.adorsys.webank.dto.TokenRequest;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.stereotype.Service;

@Service
public interface TokenServiceApi {
    String requestRecoveryToken ( TokenRequest tokenRequest);

}