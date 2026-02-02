package org.viators.personalfinanceapp.dto.item.response;

import org.viators.personalfinanceapp.dto.priceobservation.response.PriceObservationSummaryResponse;
import org.viators.personalfinanceapp.model.Item;

import java.util.List;

public record ItemSummaryResponse(
        String name,
        String description,
        List<PriceObservationSummaryResponse> priceObservationSummaryResponse
){
    public static ItemSummaryResponse from(Item item) {
        return new ItemSummaryResponse(
                item.getName(),
                item.getDescription(),
                PriceObservationSummaryResponse.listOfSummaries(item.getPriceObservations())
        );
    }

    public static List<ItemSummaryResponse> listOfSummaries(List<Item> items) {
        return items.stream()
                .map(ItemSummaryResponse::from)
                .toList();
    }
}
