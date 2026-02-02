package org.viators.personalfinanceapp.dto.item.response;

import org.viators.personalfinanceapp.dto.category.response.CategorySummaryResponse;
import org.viators.personalfinanceapp.dto.pricealert.response.PriceAlertSummaryResponse;
import org.viators.personalfinanceapp.dto.pricecomparison.response.PriceComparisonSummaryResponse;
import org.viators.personalfinanceapp.dto.priceobservation.response.PriceObservationSummaryResponse;
import org.viators.personalfinanceapp.dto.user.response.UserSummaryResponse;
import org.viators.personalfinanceapp.model.Item;
import org.viators.personalfinanceapp.model.enums.ItemUnitEnum;

import java.time.LocalDateTime;
import java.util.List;

public record ItemDetailsResponse(
        String uuid,
        String name,
        String description,
        ItemUnitEnum itemUnit,
        String brand,
        boolean isFavorite,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UserSummaryResponse user,
        List<CategorySummaryResponse> categories,
        List<PriceObservationSummaryResponse> priceObservations,
        List<PriceAlertSummaryResponse> priceAlerts,
        List<PriceComparisonSummaryResponse> priceComparisons
) {
    public static ItemDetailsResponse from(Item item) {
        return new ItemDetailsResponse(
                item.getUuid(),
                item.getName(),
                item.getDescription(),
                item.getItemUnit(),
                item.getBrand(),
                item.getIsFavorite(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                UserSummaryResponse.from(item.getUser()),
                CategorySummaryResponse.listOfSummaries(item.getCategories()),
                PriceObservationSummaryResponse.listOfSummaries(item.getPriceObservations()),
                PriceAlertSummaryResponse.listOfSummaries(item.getPriceAlerts()),
                PriceComparisonSummaryResponse.listOfSummaries(item.getPriceComparisons())
        );
    }
}
