package org.viators.personalfinanceapp.dto.pricecomparison.response;

import org.viators.personalfinanceapp.model.PriceComparison;

import java.time.LocalDate;

public record PriceComparisonSummaryResponse(
        LocalDate comparisonDate,
        String itemName,
        String bestStoreName
) {
    public static PriceComparisonSummaryResponse from(PriceComparison entity) {
        return new PriceComparisonSummaryResponse(
                entity.getComparisonDate(),
                entity.getItem().getName(),
                entity.getBestStore().getName()
        );
    }
}