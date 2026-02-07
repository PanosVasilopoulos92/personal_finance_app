package org.viators.personalfinanceapp.dto.shoppinglist.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.viators.personalfinanceapp.model.ShoppingList;

public record CreateShoppingListRequest(
        @NotBlank(message = "Name is blank")
        @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters long")
        @NotNull(message = "name is required")
        String name,

        @Size(min = 3, max = 300, message = "Description must be between 3 and 300 characters")
        String description
) {
        public ShoppingList toEntity() {
                ShoppingList shoppingList = new ShoppingList();
                shoppingList.setName(name);
                shoppingList.setDescription(description);
                return shoppingList;
        }
}
