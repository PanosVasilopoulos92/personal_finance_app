package org.viators.personalfinanceapp.dto.item.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.viators.personalfinanceapp.dto.priceobservation.request.CreatePriceObservationRequest;

public record UpdateItemPrice(
        @NotBlank(message = "Uuid cannot be null")
        String uuid,
        @Valid
        CreatePriceObservationRequest createPriceObservationRequest
) {
}
