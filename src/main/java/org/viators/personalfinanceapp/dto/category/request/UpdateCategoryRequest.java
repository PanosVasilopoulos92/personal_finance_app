package org.viators.personalfinanceapp.dto.category.request;

import org.viators.personalfinanceapp.model.Category;

import java.util.Optional;

public record UpdateCategoryRequest(
        String newName,
        String description
) {

    public void updateFields(Category category) {
        Optional.ofNullable(newName).ifPresent(category::setName);
        Optional.ofNullable(description).ifPresent(category::setDescription);
    }
}
