package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.dto.AccountRecoveryResponse;
import com.adorsys.webank.security.CertGeneratorHelper;
import com.adorsys.webank.service.AccountRecoveryValidationRequestServiceApi;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

@Service
public class AccountRecoveryValidationRequestServiceImpl implements AccountRecoveryValidationRequestServiceApi {

    private final CertGeneratorHelper certGeneratorHelper;
    private final AccountCertificateService accountCertificateService;

    @Autowired
    public AccountRecoveryValidationRequestServiceImpl(CertGeneratorHelper certGeneratorHelper,
                                                       AccountCertificateService accountCertificateService, AccountCertificateService accountCertificateService1) {
        this.certGeneratorHelper = certGeneratorHelper;
        this.accountCertificateService = accountCertificateService;
    }

    @Override
    public AccountRecoveryResponse processRecovery(JWK publicKey, String newAccountId, String recoveryJwt) {
        try {
            // Parse the recovery JWT
            SignedJWT signedJWT = SignedJWT.parse(recoveryJwt);
            String oldAccountId = getOldAccountId(newAccountId, signedJWT);

            // Generate a new KYC certificate
            String newKycCertificate = certGeneratorHelper.generateCertificate(publicKey.toJSONString());

            // Generate a new account certificate
            String newAccountCertificate = accountCertificateService.generateBankAccountCertificate(
                    publicKey.toJSONString(), oldAccountId);

            // Return a successful response
            return new AccountRecoveryResponse(oldAccountId, newKycCertificate, newAccountCertificate, "Account recovery successful");

        } catch (ParseException e) {
            // Handle invalid JWT format
            return new AccountRecoveryResponse(null, null, null, "Invalid RecoveryJWT format");
        } catch (IllegalArgumentException e) {
            // Handle specific business logic errors (e.g., token expired, account ID mismatch)
            return new AccountRecoveryResponse(null, null, null, e.getMessage());
        } catch (Exception e) {
            // Handle unexpected errors
            return new AccountRecoveryResponse(null, null, null, "An unexpected error occurred: " + e.getMessage());
        }
    }

    private static String getOldAccountId(String newAccountId, SignedJWT signedJWT) throws ParseException {
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

        // Extract and validate the TimeStamp claim
        Date issuedAt = new Date((Long) claimsSet.getClaim("TimeStamp"));
        long elapsedTime = (new Date().getTime() - issuedAt.getTime()) / (1000 * 60 * 60 * 24); // Days

        if (elapsedTime > 5) {
            throw new IllegalArgumentException("Recovery token expired");
        }

        // Extract and validate the ClaimingAccountID
        String claimingAccountId = claimsSet.getStringClaim("ClaimingAccountID");
        if (!newAccountId.equals(claimingAccountId)) {
            throw new IllegalArgumentException("Claiming account ID mismatch");
        }

        // Restore the old account (assuming a method to find the account by ID)
        return claimsSet.getStringClaim("OldAccountID");
    }
}