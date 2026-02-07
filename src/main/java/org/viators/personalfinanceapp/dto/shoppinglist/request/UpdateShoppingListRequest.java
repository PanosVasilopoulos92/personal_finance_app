package org.viators.personalfinanceapp.dto.shoppinglist.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.viators.personalfinanceapp.model.ShoppingList;

import java.util.Optional;

public record UpdateShoppingListRequest(

        @NotBlank(message = "Name is blank")
        @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters long")
        @NotNull(message = "name is required")
        String newName,

        @Size(min = 3, max = 300, message = "Description must be between 3 and 300 characters")
        String description
) {

    public ShoppingList update(ShoppingList entity) {
        Optional.ofNullable(newName).ifPresent(entity::setName);
        Optional.ofNullable(description).ifPresent(entity::setDescription);
        return entity;
    }
}
