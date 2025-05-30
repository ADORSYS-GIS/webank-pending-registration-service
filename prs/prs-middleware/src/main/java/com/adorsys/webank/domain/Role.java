package com.adorsys.webank.domain;
import lombok.Getter;

@Getter
public enum Role {
    ACCOUNT_CERTIFIED("ROLE_ACCOUNT_CERTIFIED");

    private final String roleName;

    Role(String roleName) {
        this.roleName = roleName;
    }

    public static Role fromRoleName(String roleName) {
        for (Role role : values()) {
            if (role.roleName.equals(roleName)) {
                return role;
            }
        }
        return null;
    }
}
