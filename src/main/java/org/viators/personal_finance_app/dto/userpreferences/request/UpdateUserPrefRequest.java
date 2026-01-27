package org.viators.personal_finance_app.dto.userpreferences.request;

import lombok.Builder;
import org.viators.personal_finance_app.model.UserPreferences;
import org.viators.personal_finance_app.model.enums.CurrencyEnum;
import org.viators.personal_finance_app.model.enums.LanguageEnum;

import java.util.List;
import java.util.Optional;

@Builder
public record UpdateUserPrefRequest(
        CurrencyEnum currency,
        LanguageEnum language,
        String location,
        Boolean notificationEnabled,
        Boolean emailAlerts
) {
    public UpdateUserPrefRequest {
        List<CurrencyEnum> availableCurrencies = List.of(CurrencyEnum.values());
        List<LanguageEnum> availableLanguages = List.of(LanguageEnum.values());

        if (currency != null && !availableCurrencies.contains(currency)) {
            throw new IllegalArgumentException("Not a valid currency provided");
        }

        if (language != null && !availableLanguages.contains(language)) {
            throw new IllegalArgumentException("Not a valid language provided");
        }
    }

    public static void resetUserPrefs(UserPreferences userPreferences) {
        userPreferences.setCurrency(CurrencyEnum.EUR);
        userPreferences.setLanguage(LanguageEnum.ENGLISH);
        userPreferences.setLocation("");
        userPreferences.setNotificationEnabled(false);
        userPreferences.setEmailAlerts(false);
    }

    public void updateUserPrefs(UserPreferences userPreferences) {
        Optional.ofNullable(currency).ifPresent(userPreferences::setCurrency);
        Optional.ofNullable(language).ifPresent(userPreferences::setLanguage);
        Optional.ofNullable(location).ifPresent(userPreferences::setLocation);
        Optional.ofNullable(notificationEnabled).ifPresent(userPreferences::setNotificationEnabled);
        Optional.ofNullable(emailAlerts).ifPresent(userPreferences::setEmailAlerts);
    }

}
