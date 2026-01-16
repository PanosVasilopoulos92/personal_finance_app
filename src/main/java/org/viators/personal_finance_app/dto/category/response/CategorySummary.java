package org.viators.personal_finance_app.dto.category.response;

import org.viators.personal_finance_app.model.Category;

import java.util.List;

public record CategorySummary(
        String name,
        String description
) {
    public static CategorySummary from (Category category){
        return new CategorySummary(
                category.getName(),
                category.getDescription()
        );
    }

    public static List<CategorySummary> listOfSummaries(List<Category> categories) {
        return categories.stream()
                .map(CategorySummary::from)
                .toList();
    }
}