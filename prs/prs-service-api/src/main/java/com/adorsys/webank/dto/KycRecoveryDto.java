package com.adorsys.webank.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class KycRecoveryDto {
    private String idNumber;
    private String expiryDate;
    private String accountId;
}
