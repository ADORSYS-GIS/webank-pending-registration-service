package com.adorsys.webank.service;

import com.adorsys.webank.dto.AccountRecoveryResponse;
import com.nimbusds.jose.jwk.JWK;

public interface AccountRecoveryValidationRequestServiceApi {
    AccountRecoveryResponse processRecovery(JWK publicKey, String newAccountId, String recoveryJwt);
}

