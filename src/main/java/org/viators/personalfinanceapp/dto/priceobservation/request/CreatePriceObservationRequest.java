package org.viators.personalfinanceapp.dto.priceobservation.request;

import jakarta.validation.constraints.*;
import org.viators.personalfinanceapp.model.PriceObservation;
import org.viators.personalfinanceapp.model.enums.CurrencyEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public record CreatePriceObservationRequest(
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        @Digits(integer = 10, fraction = 2, message = "Price must have at most 2 decimal places")
        BigDecimal price,

        @NotNull( message = "Currency is required")
        CurrencyEnum currency,

        @NotNull(message = "Date of observation is required")
        @PastOrPresent(message = "Observation date cannot be in the future")
        LocalDate observationDate,

        @NotBlank(message = "Location is required")
        @Size(max = 100, message = "Location must not exceed 100 characters")
        String location,

        @Size(max = 400, message = "Notes must not exceed 400 characters")
        String notes,  // Optional field

        @NotNull(message = "Item ID is required")
        Long itemId,

        @NotNull(message = "Store ID is required")
        Long storeId
) {
    public PriceObservation toEntity() {
        PriceObservation priceObservation = new PriceObservation();
        Optional.ofNullable(price).ifPresent(priceObservation::setPrice);
        Optional.ofNullable(currency).ifPresent(priceObservation::setCurrency);
        Optional.ofNullable(observationDate).ifPresent(priceObservation::setObservationDate);
        Optional.ofNullable(location).ifPresent(priceObservation::setLocation);
        Optional.ofNullable(notes).ifPresent(priceObservation::setNotes);

        return priceObservation;
    }
}
