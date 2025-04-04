package com.adorsys.webank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String accountId;
    private String fullName;
    private String profession;
    private String idNumber;
    private String dob;
    private String region;
    private String expirationDate;
    private String location;
    private String email;
    private String status;  // Ensure to convert Enum to String using .name()
    private String frontID;
    private String backID;
    private String selfie;
    private String taxDocument;
}