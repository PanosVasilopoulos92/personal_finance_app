package org.viators.personalfinanceapp.dto.item.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.viators.personalfinanceapp.dto.priceobservation.request.CreatePriceObservationRequest;
import org.viators.personalfinanceapp.model.Item;
import org.viators.personalfinanceapp.model.enums.ItemUnitEnum;

import java.util.Optional;

public record CreateItemRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 30, message = "Name must be between 2 and 30 characters")
        String name,

        @Size(min = 5, max = 300, message = "Description must be between 5 and 300 characters")
        String description,
        ItemUnitEnum itemUnit, // Throws 'InvalidFormatException' if user provide not available option

        @Size(min = 2, max = 50, message = "Brand must be between 2 and 50 characters")
        String brand,

        @NotBlank(message = "Store's name is required")
        String storeName,

        @Valid
        CreatePriceObservationRequest createPriceObservationRequest
) {

    public Item toEntity() {
        Item entity = new Item();
        Optional.ofNullable(name).ifPresent(entity::setName);
        Optional.ofNullable(description).ifPresent(entity::setDescription);
        Optional.ofNullable(itemUnit).ifPresent(entity::setItemUnit);
        Optional.ofNullable(brand).ifPresent(entity::setBrand);

        return entity;
    }
}
