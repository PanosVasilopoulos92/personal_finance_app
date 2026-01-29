package org.viators.personalfinanceapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.viators.personalfinanceapp.dto.category.request.CreateCategoryRequest;
import org.viators.personalfinanceapp.dto.category.request.UpdateCategoryRequest;
import org.viators.personalfinanceapp.dto.category.response.CategoryDetailsResponse;
import org.viators.personalfinanceapp.dto.category.response.CategorySummaryResponse;
import org.viators.personalfinanceapp.exceptions.DuplicateResourceException;
import org.viators.personalfinanceapp.exceptions.ResourceNotFoundException;
import org.viators.personalfinanceapp.model.Category;
import org.viators.personalfinanceapp.model.User;
import org.viators.personalfinanceapp.model.enums.StatusEnum;
import org.viators.personalfinanceapp.repository.CategoryRepository;
import org.viators.personalfinanceapp.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryDetailsResponse getCategoryWithDetails(String userUuid, String categoryUuid) {
        Category result = categoryRepository.findCategoryWithRelationships(userUuid, categoryUuid)
                .orElseThrow(() -> new ResourceNotFoundException("No category found for this currentUser with that name"));

        return CategoryDetailsResponse.from(result);
    }

    /**
     * Gets paginated items for a user.
     *
     * <p> Page.map() method transforms each Category entity to an CategorySummaryResponse DTO
     * while preserving all pagination metadata (total elements, page info, etc.).</p>
     *
     * @param userUuid the owner's ID
     * @param pageable pagination parameters
     * @return page of category DTOs
     */
    public Page<CategorySummaryResponse> getCategories(String userUuid, Pageable pageable) {
        return categoryRepository.findByUser_Uuid(userUuid, pageable)
                .map(CategorySummaryResponse::from);
    }

    @Transactional
    public CategorySummaryResponse create(String userUuid, CreateCategoryRequest request) {
        if (categoryRepository.existsByNameAndUser_UuidAndStatus(request.name(), userUuid, StatusEnum.ACTIVE.getCode())) {
            throw new DuplicateResourceException("There is already one category with same name for currentUser");
        }

        User user = userRepository.findByUuidAndStatus(userUuid, StatusEnum.ACTIVE.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("User does not exist or is inactive"));

        Category categoryToCreate = request.toEntity();

        categoryToCreate.addUser(user);

        categoryRepository.save(categoryToCreate);
        return CategorySummaryResponse.from(categoryToCreate);
    }

    @Transactional
    public CategorySummaryResponse update(String userUuid, String categoryUuid, UpdateCategoryRequest request) {
        Category categoryToUpdate = categoryRepository.findByUuidAndUser_UuidAndStatus(categoryUuid, userUuid, StatusEnum.ACTIVE.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("No category found with this uuid for this currentUser"));

        request.updateFields(categoryToUpdate);
        return CategorySummaryResponse.from(categoryToUpdate);
    }

    @Transactional
    public void archiveCategory(String uuid) {
        Category categoryToArchive = categoryRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No category exist with this uuid"));

        categoryToArchive.setStatus(StatusEnum.INACTIVE.getCode());
    }


}
