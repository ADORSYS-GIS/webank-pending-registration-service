package com.adorsys.webank.service;

import org.springframework.stereotype.Service;

import com.adorsys.webank.dto.TokenRequest;

@Service
public interface TokenServiceApi {
    String requestRecoveryToken ( TokenRequest tokenRequest);

}