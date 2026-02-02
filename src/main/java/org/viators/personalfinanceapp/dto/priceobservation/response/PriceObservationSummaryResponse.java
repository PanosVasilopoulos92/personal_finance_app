package org.viators.personalfinanceapp.dto.priceobservation.response;

import org.viators.personalfinanceapp.model.PriceObservation;
import org.viators.personalfinanceapp.model.enums.CurrencyEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PriceObservationSummaryResponse(
        BigDecimal price,
        CurrencyEnum currency,
        LocalDate observationDate
) {
    public static PriceObservationSummaryResponse from(PriceObservation priceObservation) {
        return new PriceObservationSummaryResponse(
                priceObservation.getPrice(),
                priceObservation.getCurrency(),
                priceObservation.getObservationDate()
        );
    }

    public static List<PriceObservationSummaryResponse> listOfSummaries(List<PriceObservation> priceObservations) {
        return priceObservations.stream()
                .map(PriceObservationSummaryResponse::from)
                .toList();
    }
}
