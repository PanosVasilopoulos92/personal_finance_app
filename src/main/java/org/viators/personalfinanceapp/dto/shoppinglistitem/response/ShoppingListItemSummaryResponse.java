package org.viators.personalfinanceapp.dto.shoppinglistitem.response;

import org.viators.personalfinanceapp.model.ShoppingListItem;
import org.viators.personalfinanceapp.model.enums.ItemUnitEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ShoppingListItemSummaryResponse(
        String uuid,
        String itemUuid,
        String itemName,
        String itemBrand,
        ItemUnitEnum itemUnit,
        String storeUuid,
        String storeName,
        BigDecimal quantity,
        Boolean isPurchased,
        BigDecimal purchasedPrice,
        LocalDate purchasedDate
) {

    public static ShoppingListItemSummaryResponse from(ShoppingListItem shoppingListItem) {
        return new ShoppingListItemSummaryResponse(
                shoppingListItem.getUuid(),
                shoppingListItem.getItem().getUuid(),
                shoppingListItem.getItem().getName(),
                shoppingListItem.getItem().getBrand(),
                shoppingListItem.getItem().getItemUnit(),
                shoppingListItem.getStore().getUuid(),
                shoppingListItem.getStore().getName(),
                shoppingListItem.getQuantity(),
                shoppingListItem.getIsPurchased(),
                shoppingListItem.getPurchasedPrice(),
                shoppingListItem.getPurchasedDate()
        );
    }

    public static List<ShoppingListItemSummaryResponse> listOfSummaries(List<ShoppingListItem> shoppingListItems) {
        return shoppingListItems.stream()
                .map(ShoppingListItemSummaryResponse::from)
                .toList();
    }
}
