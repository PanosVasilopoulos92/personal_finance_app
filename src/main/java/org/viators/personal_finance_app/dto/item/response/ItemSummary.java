package org.viators.personal_finance_app.dto.item.response;

import org.viators.personal_finance_app.model.Item;

import java.util.List;

public record ItemSummary(
        String name,
        String description
){
    public static ItemSummary from(Item item) {
        return new ItemSummary(
                item.getName(),
                item.getDescription()
        );
    }

    public static List<ItemSummary> listOfSummaries(List<Item> items) {
        return items.stream()
                .map(ItemSummary::from)
                .toList();
    }
}
