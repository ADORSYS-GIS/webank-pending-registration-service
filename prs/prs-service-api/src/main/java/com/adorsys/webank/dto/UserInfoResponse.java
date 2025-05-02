package com.adorsys.webank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String accountId;
    private String idNumber;
    private String expirationDate;
    private String location;
    private String email;
    private String status;
    private String frontID;
    private String backID;
    private String selfie;
    private String taxDocument;
    private String rejectionReason;
}