package org.viators.personalfinanceapp.dto.userpreferences.response;

import org.viators.personalfinanceapp.dto.store.response.StoreSummaryResponse;
import org.viators.personalfinanceapp.model.UserPreferences;
import org.viators.personalfinanceapp.model.enums.CurrencyEnum;

import java.util.Set;

public record UserPreferencesSummaryResponse(
        CurrencyEnum currency,
        String location,
        Boolean notificationEnabled,
        Boolean emailAlerts,
        Set<StoreSummaryResponse> preferredStoreIds
) {
    public static UserPreferencesSummaryResponse from(UserPreferences userPreferences) {
        return new UserPreferencesSummaryResponse(
                userPreferences.getCurrency(),
                userPreferences.getLocation(),
                userPreferences.getNotificationEnabled(),
                userPreferences.getEmailAlerts(),
                StoreSummaryResponse.fromList(userPreferences.getPreferredStores())
        );
    }
}