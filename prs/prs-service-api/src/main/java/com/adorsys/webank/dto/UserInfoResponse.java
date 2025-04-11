package com.adorsys.webank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

}