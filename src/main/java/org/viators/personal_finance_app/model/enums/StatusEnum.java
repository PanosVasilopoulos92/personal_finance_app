package org.viators.personal_finance_app.model.enums;

import lombok.Getter;

@Getter
public enum StatusEnum {
    ACTIVE("1", "Active"),
    INACTIVE("0", "Inactive");

    private final String code;
    private final String description;

    StatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
