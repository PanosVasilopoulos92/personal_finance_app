package org.viators.personal_finance_app.dtos;

import org.viators.personal_finance_app.model.Item;
import org.viators.personal_finance_app.model.PriceComparison;

import java.time.LocalDate;

public class PriceComparisonDTOs {

    public record PriceComparisonSummary(
            LocalDate comparisonDate,
            String itemName,
            String bestStoreName
    ) {
        public static PriceComparisonSummary from(PriceComparison entity) {
            return new PriceComparisonSummary(
                    entity.getComparisonDate(),
                    entity.getItem().getName(),
                    entity.getBestStore().getName()
            );
        }
    }
}
