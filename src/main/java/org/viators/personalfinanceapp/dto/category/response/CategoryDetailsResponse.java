package org.viators.personalfinanceapp.dto.category.response;

import org.viators.personalfinanceapp.dto.item.response.ItemSummaryResponse;
import org.viators.personalfinanceapp.dto.user.response.UserSummaryResponse;
import org.viators.personalfinanceapp.model.Category;

import java.util.List;

public record CategoryDetailsResponse(
        String name,
        String description,
        UserSummaryResponse user,
        List<ItemSummaryResponse> items
) {
    public static CategoryDetailsResponse from(Category category) {
        return new CategoryDetailsResponse(
                category.getName(),
                category.getDescription(),
                UserSummaryResponse.from(category.getUser()),
                ItemSummaryResponse.listOfSummaries(category.getItems())
        );
    }
}
