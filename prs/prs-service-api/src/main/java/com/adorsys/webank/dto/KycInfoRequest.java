package com.adorsys.webank.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KycInfoRequest {

    private String idNumber;
    private String expiryDate;
    private String accountId;
    private String rejectionReason;
    private String rejectionNote;

    public KycInfoRequest(String idNumber, String expiryDate, String accountId, String rejectionReason) {
        this.idNumber = idNumber;
        this.expiryDate = expiryDate;
        this.accountId = accountId;
        this.rejectionReason = rejectionReason;
    }
}
