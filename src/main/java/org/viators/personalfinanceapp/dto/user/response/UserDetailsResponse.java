package org.viators.personalfinanceapp.dto.user.response;

import org.viators.personalfinanceapp.dto.basket.response.BasketSummaryResponse;
import org.viators.personalfinanceapp.dto.category.response.CategorySummaryResponse;
import org.viators.personalfinanceapp.dto.inflationreport.response.InflationReportSummaryResponse;
import org.viators.personalfinanceapp.dto.item.response.ItemSummaryResponse;
import org.viators.personalfinanceapp.dto.pricealert.response.PriceAlertSummaryResponse;
import org.viators.personalfinanceapp.dto.shoppinglist.response.ShoppingListSummaryResponse;
import org.viators.personalfinanceapp.dto.userpreferences.response.UserPreferencesSummaryResponse;
import org.viators.personalfinanceapp.model.User;
import org.viators.personalfinanceapp.model.enums.UserRolesEnum;

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
        UserPreferencesSummaryResponse userPreferences,
        List<ItemSummaryResponse> items,
        List<CategorySummaryResponse> categories,
        List<PriceAlertSummaryResponse> priceAlerts,
        List<ShoppingListSummaryResponse> shoppingLists,
        List<InflationReportSummaryResponse> inflationReports,
        List<BasketSummaryResponse> baskets
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
                UserPreferencesSummaryResponse.from(user.getUserPreferences()),
                ItemSummaryResponse.listOfSummaries(user.getItems()),
                CategorySummaryResponse.listOfSummaries(user.getCategories()),
                PriceAlertSummaryResponse.listOfSummaries(user.getPriceAlerts()),
                ShoppingListSummaryResponse.listOfSummaries(user.getShoppingLists()),
                InflationReportSummaryResponse.listOfSummaries(user.getInflationReports()),
                BasketSummaryResponse.listOfSummaries(user.getBaskets())
        );
    }
}