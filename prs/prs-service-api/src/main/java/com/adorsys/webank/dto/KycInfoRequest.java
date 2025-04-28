package com.adorsys.webank.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycInfoRequest {

    private String idNumber;
    private String expiryDate;
    private String accountId;

    public KycInfoRequest(String idNumber, String expiryDate , String accountId) {

        this.idNumber = idNumber;
        this.expiryDate = expiryDate;
        this.accountId = accountId;
    }
}
