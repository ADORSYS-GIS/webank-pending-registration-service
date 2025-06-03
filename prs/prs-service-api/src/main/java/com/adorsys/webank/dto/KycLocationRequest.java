package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KycLocationRequest {
    private String location;
    private String accountId;

    public KycLocationRequest(String location, String accountId) {
        this.location = location;
        this.accountId = accountId;
    }



}