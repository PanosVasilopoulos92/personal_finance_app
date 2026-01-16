package org.viators.personal_finance_app.dto.userpreferences.response;

import org.viators.personal_finance_app.model.UserPreferences;
import org.viators.personal_finance_app.model.enums.CurrencyEnum;

public record UserPreferencesSummary(
        CurrencyEnum defaultCurrency,
        String defaultLocation,
        Boolean notificationEnabled,
        Boolean emailAlerts,
        String preferredStoreIds
) {
    public static UserPreferencesSummary from(UserPreferences userPreferences) {
        return new UserPreferencesSummary(
                userPreferences.getDefaultCurrency(),
                userPreferences.getDefaultLocation(),
                userPreferences.getNotificationEnabled(),
                userPreferences.getEmailAlerts(),
                userPreferences.getPreferredStoreIds()
        );
    }
}