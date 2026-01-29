package org.viators.personalfinanceapp.dto.category.response;

import org.viators.personalfinanceapp.model.Category;

import java.util.List;

public record CategorySummaryResponse(
        String name,
        String description
) {
    public static CategorySummaryResponse from (Category category){
        return new CategorySummaryResponse(
                category.getName(),
                category.getDescription()
        );
    }

    public static List<CategorySummaryResponse> listOfSummaries(List<Category> categories) {
        return categories.stream()
                .map(CategorySummaryResponse::from)
                .toList();
    }
}