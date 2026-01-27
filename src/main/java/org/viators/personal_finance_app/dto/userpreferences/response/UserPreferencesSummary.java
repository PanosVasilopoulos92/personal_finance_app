package org.viators.personal_finance_app.dto.userpreferences.response;

import org.viators.personal_finance_app.dto.store.response.StoreSummary;
import org.viators.personal_finance_app.model.UserPreferences;
import org.viators.personal_finance_app.model.enums.CurrencyEnum;

import java.util.Set;

public record UserPreferencesSummary(
        CurrencyEnum currency,
        String location,
        Boolean notificationEnabled,
        Boolean emailAlerts,
        Set<StoreSummary> preferredStoreIds
) {
    public static UserPreferencesSummary from(UserPreferences userPreferences) {
        return new UserPreferencesSummary(
                userPreferences.getCurrency(),
                userPreferences.getLocation(),
                userPreferences.getNotificationEnabled(),
                userPreferences.getEmailAlerts(),
                StoreSummary.fromList(userPreferences.getPreferredStores())
        );
    }
}