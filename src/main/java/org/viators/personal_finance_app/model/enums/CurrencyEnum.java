package org.viators.personal_finance_app.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CurrencyEnum {
    EUR("Euro", "€"),
    USD("US Dollar", "$");

    private final String description;
    private final String symbol;
}
