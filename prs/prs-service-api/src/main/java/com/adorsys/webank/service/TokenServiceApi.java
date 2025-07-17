package com.adorsys.webank.service;

import com.adorsys.webank.dto.response.TokenResponse;
import org.springframework.stereotype.Service;

import com.adorsys.webank.dto.TokenRequest;

@Service
public interface TokenServiceApi {
    TokenResponse requestRecoveryToken (TokenRequest tokenRequest);

}