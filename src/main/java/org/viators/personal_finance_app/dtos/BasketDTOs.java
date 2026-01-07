package org.viators.personal_finance_app.dtos;

import org.viators.personal_finance_app.model.Basket;

import java.util.List;

public class BasketDTOs {

    public record BasketSummary(
            String name,
            String description,
            Integer itemsCount
    ) {
        public BasketSummary {
            if (itemsCount == null) {
                itemsCount = 0;
            }
        }

        public static BasketSummary from(Basket basket) {
            return new BasketSummary(
                    basket.getName(),
                    basket.getDescription(),
                    basket.getBasketItems().size()
            );
        }

        public static List<BasketSummary> listOfSummaries(List<Basket> baskets) {
            return baskets.stream()
                    .map(BasketSummary::from)
                    .toList();
        }
    }
}
