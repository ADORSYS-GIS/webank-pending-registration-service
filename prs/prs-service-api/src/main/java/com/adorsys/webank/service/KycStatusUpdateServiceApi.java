package com.adorsys.webank.service;

import com.adorsys.webank.dto.response.KycStatusUpdateResponse;
import org.springframework.stereotype.Service;

/**
 * Service interface for updating KYC status
 */
@Service
public interface KycStatusUpdateServiceApi {
    /**
     * Updates the KYC status for the specified account.
     *
     * @param accountId The account ID for which to update the KYC status
     * @param newStatus The new status to set (e.g., APPROVED, REJECTED, PENDING)
     * @param idNumber The document ID number for verification
     * @param expiryDate The document expiry date for verification
     * @param rejectionReason Reason for rejection if status is REJECTED
     * @return KycStatusUpdateResponse containing the result of the operation
     */
    KycStatusUpdateResponse updateKycStatus(String accountId, String newStatus, String idNumber, String expiryDate, String rejectionReason);
}
