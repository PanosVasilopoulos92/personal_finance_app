package org.viators.personalfinanceapp.dto.item.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.viators.personalfinanceapp.model.Item;
import org.viators.personalfinanceapp.model.enums.ItemUnitEnum;

import java.util.Optional;

public record UpdateItemRequest(
        @NotBlank(message = "User's uuid is required for update operation")
        @Size(min = 10, max = 50 , message = "Uuid must be 10-50 chars")
        String userUuid,
        String itemUuid,
        String categoryUuid,
        String newName,
        String description,
        ItemUnitEnum itemUnit,
        String brand,
        Boolean isFavorite
) {

    public void updateItem(Item item) {
        Optional.ofNullable(newName).ifPresent(item::setName);
        Optional.ofNullable(description).ifPresent(item::setDescription);
        Optional.ofNullable(itemUnit).ifPresent(item::setItemUnit);
        Optional.ofNullable(brand).ifPresent(item::setBrand);
        Optional.ofNullable(isFavorite).ifPresent(item::setIsFavorite);

    }
}
