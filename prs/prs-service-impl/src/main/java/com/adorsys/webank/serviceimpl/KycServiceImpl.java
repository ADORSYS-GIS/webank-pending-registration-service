package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.KycDocumentRequest;
import com.adorsys.webank.dto.KycEmailRequest;
import com.adorsys.webank.dto.KycInfoRequest;
import com.adorsys.webank.dto.KycLocationRequest;
import com.adorsys.webank.exceptions.FailedToSendOTPException;
import com.adorsys.webank.exceptions.HashComputationException;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.service.KycServiceApi;
import com.nimbusds.jose.jwk.JWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class KycServiceImpl implements KycServiceApi {

    private static final Logger log = LoggerFactory.getLogger(KycServiceImpl.class);

    private final OtpRequestRepository otpRequestRepository;

    public KycServiceImpl(OtpRequestRepository otpRequestRepository) {
        this.otpRequestRepository = otpRequestRepository;
    }


    @Override
    public String sendKycDocument(JWK devicePub, KycDocumentRequest kycDocumentRequest) {
        if (kycDocumentRequest == null) {
            throw new IllegalArgumentException("Invalid KYC Document Request");
        }

        try {
            log.info("OTP sent to device for KYC Document: {}");

            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);

            return "KYC Document sent successfully.";
        } catch (Exception e) {
            log.error("Failed to send KYC Document", e);
            throw new FailedToSendOTPException("Failed to send KYC Document");
        }
    }

    @Override
    public String sendKycinfo(JWK devicePub, KycInfoRequest kycInfoRequest) {
        if (kycInfoRequest == null) {
            throw new IllegalArgumentException("Invalid KYC Info Request");
        }

        try {
            log.info("OTP sent to device for KYC Info: {}");

            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);

            return "KYC Info sent successfully.";
        } catch (Exception e) {
            log.error("Failed to send KYC Info", e);
            throw new FailedToSendOTPException("Failed to send KYC Info");
        }
    }

    @Override
    public String sendKyclocation(JWK devicePub, KycLocationRequest kycLocationRequest) {
        if (kycLocationRequest == null) {
            throw new IllegalArgumentException("Invalid KYC Location Request");
        }

        try {
            log.info("OTP sent to device for KYC Location: {}");

            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);

            return "KYC Location sent successfully.";
        } catch (Exception e) {
            log.error("Failed to send KYC Location", e);
            throw new FailedToSendOTPException("Failed to send KYC Location");
        }
    }

    @Override
    public String sendKycEmail(JWK devicePub, KycEmailRequest kycEmailRequest) {
        if (kycEmailRequest == null) {
            throw new IllegalArgumentException("Invalid KYC Email Request");
        }

        try {
            log.info("OTP sent to device for KYC Email: {}");

            String devicePublicKey = devicePub.toJSONString();
            String publicKeyHash = computePublicKeyHash(devicePublicKey);

            return "KYC Email sent successfully.";
        } catch (Exception e) {
            log.error("Failed to send KYC Email", e);
            throw new FailedToSendOTPException("Failed to send KYC Email");
        }
    }

    public String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new HashComputationException("Error computing hash");
        }
    }

    private String computePublicKeyHash(String devicePublicKey) {
        return computeHash(devicePublicKey);
    }

}
