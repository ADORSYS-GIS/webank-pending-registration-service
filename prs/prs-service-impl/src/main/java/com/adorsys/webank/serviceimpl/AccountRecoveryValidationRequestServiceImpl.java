package com.adorsys.webank.serviceimpl;

import com.adorsys.error.ValidationException;
import com.adorsys.webank.config.CertGeneratorHelper;
import com.adorsys.webank.dto.AccountRecoveryResponse;
import com.adorsys.webank.service.AccountRecoveryValidationRequestServiceApi;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import  com.nimbusds.jose.jwk.ECKey;
import com.adorsys.webank.config.SecurityUtils;
import com.adorsys.webank.config.JwtValidator;
import  java.util.Optional;
import org.apache.coyote.BadRequestException;

import java.text.ParseException;

@Service
@RequiredArgsConstructor
public class AccountRecoveryValidationRequestServiceImpl implements AccountRecoveryValidationRequestServiceApi {

    private final CertGeneratorHelper certGeneratorHelper;

    @Override
    public AccountRecoveryResponse processRecovery(String newAccountId) {

        ECKey publicKey = SecurityUtils.extractDeviceJwkFromContext();
        String jwtToken =extractJwtToken();
        String recoveryJwt = JwtValidator.extractClaim(jwtToken, "recoveryJwt");

        validateInput(newAccountId, recoveryJwt);
        
        try {

            if (recoveryJwt == null || recoveryJwt.isEmpty()) {
                throw new BadRequestException("Invalid request. Missing recoveryJwt.");
            }
            SignedJWT signedJWT = parseRecoveryJwt(recoveryJwt);
            String oldAccountId = getOldAccountId(newAccountId, signedJWT);
            String newKycCertificate = generateNewKycCertificate(publicKey);
            return createSuccessResponse(oldAccountId, newKycCertificate);
        } catch (ParseException e) {
            throw new ValidationException("Invalid RecoveryJWT format");
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (Exception e) {
            throw new ValidationException("An unexpected error occurred: " + e.getMessage());
        }
    }

    private void validateInput(String newAccountId, String recoveryJwt) {
        if (newAccountId == null || newAccountId.isEmpty()) {
            throw new ValidationException("New account ID is required");
        }
        if (recoveryJwt == null || recoveryJwt.isEmpty()) {
            throw new ValidationException("Recovery JWT is required");
        }
    }

    private SignedJWT parseRecoveryJwt(String recoveryJwt) throws ParseException {
        return SignedJWT.parse(recoveryJwt);
    }

    private String generateNewKycCertificate(JWK publicKey) {
        return certGeneratorHelper.generateCertificate(publicKey.toJSONString());
    }

    private AccountRecoveryResponse createSuccessResponse(String oldAccountId, String newKycCertificate) {
        return new AccountRecoveryResponse(oldAccountId, newKycCertificate, "Account recovery successful");
    }

    private static String getOldAccountId(String newAccountId, SignedJWT signedJWT) throws ParseException {
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

        // Extract and validate the ClaimingAccountID
        String claimingAccountId = claimsSet.getStringClaim("newAccountId");
        if (!newAccountId.equals(claimingAccountId)) {
            throw new ValidationException("Claiming account ID mismatch");
        }

        // Restore the old account (assuming a method to find the account by ID)
        return claimsSet.getStringClaim("oldAccountId");
    }

    private String extractJwtToken() {
        Optional<String> jwtOpt = SecurityUtils.getCurrentUserJWT();
        if (jwtOpt.isEmpty()) {
            throw new IllegalStateException("No JWT token found in security context");
        }
        return jwtOpt.get();
    }
}