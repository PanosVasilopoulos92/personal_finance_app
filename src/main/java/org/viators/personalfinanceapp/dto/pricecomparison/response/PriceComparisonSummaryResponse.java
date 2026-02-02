package org.viators.personalfinanceapp.dto.pricecomparison.response;

import org.viators.personalfinanceapp.model.PriceComparison;

import java.time.LocalDate;
import java.util.List;

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

    public static List<PriceComparisonSummaryResponse> listOfSummaries(List<PriceComparison> priceComparisons) {
        return priceComparisons.stream()
                .map(PriceComparisonSummaryResponse::from)
                .toList();
    }
}