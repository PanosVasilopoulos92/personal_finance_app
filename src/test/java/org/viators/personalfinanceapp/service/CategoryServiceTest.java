package org.viators.personalfinanceapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.viators.personalfinanceapp.dto.category.request.CreateCategoryRequest;
import org.viators.personalfinanceapp.dto.category.response.CategorySummaryResponse;
import org.viators.personalfinanceapp.model.Category;
import org.viators.personalfinanceapp.model.User;
import org.viators.personalfinanceapp.model.enums.StatusEnum;
import org.viators.personalfinanceapp.model.enums.UserRolesEnum;
import org.viators.personalfinanceapp.repository.CategoryRepository;
import org.viators.personalfinanceapp.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Category Service Test")
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;
    private Category category;
    private CreateCategoryRequest request;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .uuid("550e8400-e29b-41d4-a716-446655440000")
                .username("johndoe")
                .email("john@example.com")
                .password("encrypted")
                .firstName("John")
                .lastName("Doe")
                .userRole(UserRolesEnum.USER)
                .status(StatusEnum.ACTIVE.getCode())
                .build();

        category = Category.builder()
                .name("Groceries")
                .description("Contains fruits and vegetables")
                .build();

        request = new CreateCategoryRequest(
                "techStaff",
                "Contains tech related items"
        );
    }

    @Test
    void createCategory_ValidRequest_SuccessfulResponse() {
        // Arrange
        when(categoryRepository.existsByNameAndUser_UuidAndStatus(request.name(), testUser.getUuid(), StatusEnum.ACTIVE.getCode())).thenReturn(false);
        when(userRepository.findByUuidAndStatus(testUser.getUuid(), StatusEnum.ACTIVE.getCode())).thenReturn(Optional.of(testUser));
        // Act
        CategorySummaryResponse response = categoryService.create(testUser.getUuid(), request);

        System.out.println(response);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("techStaff");
    }
}
