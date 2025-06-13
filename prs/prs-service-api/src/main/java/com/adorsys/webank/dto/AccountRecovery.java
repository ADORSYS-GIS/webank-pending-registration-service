package com.adorsys.webank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Account recovery request data")
public class AccountRecovery {
    @Schema(description = "New account ID to associate with the recovered account", 
            example = "acc_123456789", 
            required = true)
    private String newAccountId;

    public AccountRecovery(String newAccountId) {
        this.newAccountId = newAccountId;
    }
}