package com.adorsys.webank.service;
import com.adorsys.webank.dto.KycDocumentRequest;
import com.adorsys.webank.dto.KycEmailRequest;
import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.dto.KycLocationRequest;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.stereotype.Service;

@Service
public interface KycServiceApi {
    String sendKycDocument(JWK devicePub, KycDocumentRequest kycDocumentRequest);
    String sendKycinfo(JWK devicePub, KycInfoRequest kycInfoRequest);
    String sendKyclocation(JWK devicePub, KycLocationRequest kycLocationRequest);
    String sendKycEmail(JWK devicePub, KycEmailRequest kycEmailRequest);
}
