package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class KycStatusUpdateDto {
    private String idNumber;
    private String expiryDate;
    private String accountId;
    private String status;
    private String rejectionReason;
}
