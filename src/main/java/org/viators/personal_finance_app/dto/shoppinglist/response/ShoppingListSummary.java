package org.viators.personal_finance_app.dto.shoppinglist.response;

import org.viators.personal_finance_app.model.ShoppingList;

import java.util.List;

public record ShoppingListSummary(
        String name,
        String description,
        int numberOfItems
) {

    public static ShoppingListSummary from(ShoppingList shoppingList) {
        return new ShoppingListSummary(
                shoppingList.getName(),
                shoppingList.getDescription(),
                shoppingList.getShoppingListItems().size()
        );
    }

    public static List<ShoppingListSummary> listOfSummaries(List<ShoppingList> shoppingLists) {
        return shoppingLists.stream()
                .map(ShoppingListSummary::from)
                .toList();
    }
}
