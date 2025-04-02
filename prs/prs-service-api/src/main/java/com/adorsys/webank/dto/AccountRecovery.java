package com.adorsys.webank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRecovery {
    private String newAccountId;

public AccountRecovery(String newAccountId) {
    this.newAccountId = newAccountId;
}
}