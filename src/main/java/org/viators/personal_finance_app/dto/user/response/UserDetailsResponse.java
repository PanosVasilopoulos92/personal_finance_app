package org.viators.personal_finance_app.dto.user.response;

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
        UserPreferenceDTOs.UserPreferencesSummary userPreferences,
        List<ItemDTOs.ItemSummary> items,
        List<CategoryDTOs.CategorySummary> categories,
        List<PriceAlertDTOs.PriceAlertSummary> priceAlerts,
        List<ShoppingListDTOs.ShoppingListSummary> shoppingLists,
        List<InflationReportDTOs.InflationReportSummary> inflationReports,
        List<BasketDTOs.BasketSummary> baskets
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
                UserPreferenceDTOs.UserPreferencesSummary.from(user.getUserPreferences()),
                ItemDTOs.ItemSummary.listOfSummaries(user.getItems()),
                CategoryDTOs.CategorySummary.listOfSummaries(user.getCategories()),
                PriceAlertDTOs.PriceAlertSummary.listOfSummaries(user.getPriceAlerts()),
                ShoppingListDTOs.ShoppingListSummary.listOfSummaries(user.getShoppingLists()),
                InflationReportDTOs.InflationReportSummary.listOfSummaries(user.getInflationReports()),
                BasketDTOs.BasketSummary.listOfSummaries(user.getBaskets())
        );
    }
}