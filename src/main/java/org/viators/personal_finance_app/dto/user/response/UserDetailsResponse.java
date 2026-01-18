package org.viators.personal_finance_app.dto.user.response;

import org.viators.personal_finance_app.dto.basket.response.BasketSummary;
import org.viators.personal_finance_app.dto.category.response.CategorySummary;
import org.viators.personal_finance_app.dto.inflationreport.response.InflationReportSummary;
import org.viators.personal_finance_app.dto.item.response.ItemSummary;
import org.viators.personal_finance_app.dto.pricealert.response.PriceAlertSummary;
import org.viators.personal_finance_app.dto.shoppinglist.response.ShoppingListSummary;
import org.viators.personal_finance_app.dto.userpreferences.response.UserPreferencesSummary;
import org.viators.personal_finance_app.model.User;
import org.viators.personal_finance_app.model.enums.UserRolesEnum;

import java.time.LocalDateTime;
import java.util.List;

public record UserDetailsResponse(
        String uuid,
        String username,
        String fullName,
        String email,
        Boolean isActive,
        UserRolesEnum userRole,
        LocalDateTime createdAt,
        UserPreferencesSummary userPreferences,
        List<ItemSummary> items,
        List<CategorySummary> categories,
        List<PriceAlertSummary> priceAlerts,
        List<ShoppingListSummary> shoppingLists,
        List<InflationReportSummary> inflationReports,
        List<BasketSummary> baskets
) {

    public static UserDetailsResponse from(User user) {
        return new UserDetailsResponse(
                user.getUsername(),
                user.getUuid(),
                user.getFirstName().concat(" ").concat(user.getLastName()),
                user.getEmail(),
                user.getStatus().equals("1"),
                user.getUserRole(),
                user.getCreatedAt(),
                UserPreferencesSummary.from(user.getUserPreferences()),
                ItemSummary.listOfSummaries(user.getItems()),
                CategorySummary.listOfSummaries(user.getCategories()),
                PriceAlertSummary.listOfSummaries(user.getPriceAlerts()),
                ShoppingListSummary.listOfSummaries(user.getShoppingLists()),
                InflationReportSummary.listOfSummaries(user.getInflationReports()),
                BasketSummary.listOfSummaries(user.getBaskets())
        );
    }
}