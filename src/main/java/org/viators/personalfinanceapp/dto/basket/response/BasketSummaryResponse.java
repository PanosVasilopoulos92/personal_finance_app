package org.viators.personalfinanceapp.dto.basket.response;

import org.viators.personalfinanceapp.model.Basket;

import java.util.List;

public record BasketSummaryResponse(
        String name,
        String description,
        Integer itemsCount
) {
    public BasketSummaryResponse {
        if (itemsCount == null) {
            itemsCount = 0;
        }
    }

    public static BasketSummaryResponse from(Basket basket) {
        return new BasketSummaryResponse(
                basket.getName(),
                basket.getDescription(),
                basket.getBasketItems().size()
        );
    }

    public static List<BasketSummaryResponse> listOfSummaries(List<Basket> baskets) {
        return baskets.stream()
                .map(BasketSummaryResponse::from)
                .toList();
    }
}
