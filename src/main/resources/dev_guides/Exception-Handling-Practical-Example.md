# 🎯 Exception Handling Practical Example

> **Goal:** See exception handling in action across all layers  
> **Feature:** Category Management (CRUD operations)  
> **Flow:** Controller → Service → Repository → Database

---

## Overview: The Complete Flow

```
Client Request: POST /api/categories { "name": "Electronics" }
        │
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│  CategoryController                                                  │
│  - Receives HTTP request                                            │
│  - @Valid triggers validation → MethodArgumentNotValidException     │
│  - Delegates to service                                             │
│  - Returns ResponseEntity                                           │
└─────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│  CategoryService                                                     │
│  - Business logic & validation                                      │
│  - Throws: ResourceNotFoundException, DuplicateResourceException    │
│  - Throws: BusinessValidationException, AccessDeniedException       │
└─────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│  CategoryRepository                                                  │
│  - Data access                                                      │
│  - May trigger: DataIntegrityViolationException                     │
└─────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│  GlobalExceptionHandler                                              │
│  - Catches ALL exceptions                                           │
│  - Transforms to ErrorResponse                                      │
│  - Returns appropriate HTTP status                                  │
└─────────────────────────────────────────────────────────────────────┘
        │
        ▼
Client receives: { "status": 409, "errorCode": "DUPLICATE_RESOURCE", ... }
```

---

## Layer 1: Entity

```java
package com.yourapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Category entity for organizing items.
 * 
 * Business Rules:
 * - Category belongs to one user (ownership)
 * - Name must be unique per user
 * - Cannot delete if items exist (must archive instead)
 */
@Entity
@Table(name = "categories", 
    uniqueConstraints = @UniqueConstraint(
        name = "uk_category_user_name", 
        columnNames = {"user_id", "name"}
    ),
    indexes = @Index(name = "idx_category_user", columnList = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean archived = false;
    
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Item> items = new ArrayList<>();
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS - Used by Service layer for business logic
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Checks if this category is owned by the given user.
     * Used for authorization checks in the service layer.
     */
    public boolean isOwnedBy(Long userId) {
        return user != null && user.getId().equals(userId);
    }
    
    /**
     * Checks if category can be deleted (no items attached).
     * Business rule: Categories with items must be archived, not deleted.
     */
    public boolean canBeDeleted() {
        return items == null || items.isEmpty();
    }
    
    /**
     * Returns count of non-archived items.
     */
    public int getActiveItemCount() {
        if (items == null) return 0;
        return (int) items.stream()
            .filter(item -> !item.getArchived())
            .count();
    }
}
```

---

## Layer 2: DTOs (Request & Response)

### Request DTOs

```java
package com.yourapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a category.
 * 
 * Validation happens BEFORE the controller method executes.
 * If validation fails → MethodArgumentNotValidException → 400 Bad Request
 */
public record CreateCategoryRequest(
    
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be 2-100 characters")
    String name,
    
    @Size(max = 500, message = "Description must be at most 500 characters")
    String description
    
) {}
```

```java
package com.yourapp.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a category.
 * All fields are optional - only provided fields are updated.
 */
public record UpdateCategoryRequest(
    
    @Size(min = 2, max = 100, message = "Category name must be 2-100 characters")
    String name,
    
    @Size(max = 500, message = "Description must be at most 500 characters")
    String description,
    
    Boolean archived
    
) {}
```

### Response DTO

```java
package com.yourapp.dto.response;

import com.yourapp.entity.Category;
import java.time.LocalDateTime;

/**
 * Response DTO for category data.
 * 
 * Key principle: NEVER return the entity directly!
 * - Entities may have sensitive data
 * - Entities may have lazy-loaded collections (serialization issues)
 * - DTOs define your API contract (decoupled from DB schema)
 */
public record CategoryResponse(
    Long id,
    String name,
    String description,
    Boolean archived,
    Integer itemCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Factory method to create response from entity.
     * Single place for entity-to-DTO conversion.
     */
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getArchived(),
            category.getActiveItemCount(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}
```

---

## Layer 3: Repository

```java
package com.yourapp.repository;

import com.yourapp.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Category entity.
 * 
 * Exception scenarios from this layer:
 * - DataIntegrityViolationException: When unique constraint violated
 * - EntityNotFoundException: When using getReference() on non-existent ID
 * 
 * Note: These exceptions bubble up to GlobalExceptionHandler automatically.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Finds category by ID ensuring user ownership.
     * Returns Optional.empty() if not found OR not owned by user.
     * 
     * Usage: Service calls this, gets Optional.empty(), throws ResourceNotFoundException
     */
    Optional<Category> findByIdAndUserId(Long id, Long userId);
    
    /**
     * Finds all non-archived categories for a user.
     */
    List<Category> findByUserIdAndArchivedFalseOrderByNameAsc(Long userId);
    
    /**
     * Finds all categories for a user (including archived).
     */
    List<Category> findByUserIdOrderByNameAsc(Long userId);
    
    /**
     * Checks if category name already exists for user.
     * Used BEFORE creating/updating to provide better error message.
     * 
     * Why check explicitly instead of relying on DB constraint?
     * - Better error message: "Category 'Electronics' already exists"
     * - vs generic: "Duplicate entry for key 'uk_category_user_name'"
     */
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    
    /**
     * Checks for duplicate name excluding current category (for updates).
     */
    @Query("""
        SELECT COUNT(c) > 0 FROM Category c 
        WHERE c.user.id = :userId 
        AND LOWER(c.name) = LOWER(:name) 
        AND c.id != :excludeId
        """)
    boolean existsByUserIdAndNameIgnoreCaseExcludingId(
        @Param("userId") Long userId,
        @Param("name") String name,
        @Param("excludeId") Long excludeId
    );
}
```

---

## Layer 4: Service (Where Most Exceptions Originate)

```java
package com.yourapp.service;

import com.yourapp.dto.request.CreateCategoryRequest;
import com.yourapp.dto.request.UpdateCategoryRequest;
import com.yourapp.dto.response.CategoryResponse;
import com.yourapp.entity.Category;
import com.yourapp.entity.User;
import com.yourapp.exception.*;
import com.yourapp.repository.CategoryRepository;
import com.yourapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for Category operations.
 * 
 * THIS IS WHERE MOST BUSINESS EXCEPTIONS ARE THROWN!
 * 
 * Exception throwing guidelines:
 * - ResourceNotFoundException: When entity not found by ID
 * - DuplicateResourceException: When unique constraint would be violated
 * - BusinessValidationException: When business rule violated
 * - AccessDeniedException: When user tries to access another user's resource
 * 
 * All exceptions bubble up to GlobalExceptionHandler automatically.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a new category for the user.
     * 
     * Possible exceptions:
     * - ResourceNotFoundException: If user doesn't exist (shouldn't happen with valid JWT)
     * - DuplicateResourceException: If category name already exists for user
     * 
     * @param userId The authenticated user's ID
     * @param request The category creation request
     * @return The created category
     */
    @Transactional
    public CategoryResponse createCategory(Long userId, CreateCategoryRequest request) {
        log.info("Creating category '{}' for user {}", request.name(), userId);
        
        // Step 1: Validate user exists
        // This shouldn't fail if JWT is valid, but defensive programming is good
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.error("User {} not found during category creation - this shouldn't happen!", userId);
                return new ResourceNotFoundException("User", userId);
            });
        
        // Step 2: Check for duplicate name
        // We check BEFORE saving to provide a better error message
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            log.debug("Category '{}' already exists for user {}", request.name(), userId);
            throw new DuplicateResourceException("Category", "name", request.name());
        }
        
        // Step 3: Create and save
        Category category = Category.builder()
            .user(user)
            .name(request.name().trim())
            .description(request.description())
            .build();
        
        Category saved = categoryRepository.save(category);
        log.info("Category created with ID {} for user {}", saved.getId(), userId);
        
        return CategoryResponse.from(saved);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Gets a category by ID.
     * 
     * Possible exceptions:
     * - ResourceNotFoundException: If category doesn't exist OR user doesn't own it
     * 
     * Note: We don't distinguish between "not found" and "not owned" for security.
     * This prevents enumeration attacks (attacker can't tell if ID exists).
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long categoryId, Long userId) {
        log.debug("Fetching category {} for user {}", categoryId, userId);
        
        Category category = findCategoryOwnedByUser(categoryId, userId);
        return CategoryResponse.from(category);
    }
    
    /**
     * Gets all categories for a user.
     * 
     * No exceptions expected - empty list is valid.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(Long userId, boolean includeArchived) {
        log.debug("Fetching categories for user {} (includeArchived={})", userId, includeArchived);
        
        List<Category> categories = includeArchived
            ? categoryRepository.findByUserIdOrderByNameAsc(userId)
            : categoryRepository.findByUserIdAndArchivedFalseOrderByNameAsc(userId);
        
        return categories.stream()
            .map(CategoryResponse::from)
            .toList();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Updates a category.
     * 
     * Possible exceptions:
     * - ResourceNotFoundException: If category doesn't exist or user doesn't own it
     * - DuplicateResourceException: If new name conflicts with existing category
     */
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, Long userId, UpdateCategoryRequest request) {
        log.info("Updating category {} for user {}", categoryId, userId);
        
        // Step 1: Find and verify ownership
        Category category = findCategoryOwnedByUser(categoryId, userId);
        
        // Step 2: Check name uniqueness if name is being changed
        if (request.name() != null && !request.name().equalsIgnoreCase(category.getName())) {
            if (categoryRepository.existsByUserIdAndNameIgnoreCaseExcludingId(
                    userId, request.name(), categoryId)) {
                log.debug("Cannot rename to '{}' - name already exists", request.name());
                throw new DuplicateResourceException("Category", "name", request.name());
            }
            category.setName(request.name().trim());
        }
        
        // Step 3: Update other fields if provided
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.archived() != null) {
            category.setArchived(request.archived());
        }
        
        // Note: No explicit save needed - JPA dirty checking handles it
        log.info("Category {} updated successfully", categoryId);
        return CategoryResponse.from(category);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Deletes a category.
     * 
     * Possible exceptions:
     * - ResourceNotFoundException: If category doesn't exist or user doesn't own it
     * - BusinessValidationException: If category has items (must archive instead)
     */
    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        log.info("Deleting category {} for user {}", categoryId, userId);
        
        // Step 1: Find and verify ownership
        Category category = findCategoryOwnedByUser(categoryId, userId);
        
        // Step 2: Check business rule - cannot delete if items exist
        if (!category.canBeDeleted()) {
            log.debug("Cannot delete category {} - has {} items", categoryId, category.getItems().size());
            throw new BusinessValidationException(
                String.format("Cannot delete category '%s' with %d items. Archive it instead.", 
                    category.getName(), 
                    category.getItems().size())
            );
        }
        
        // Step 3: Delete
        categoryRepository.delete(category);
        log.info("Category {} deleted successfully", categoryId);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Finds a category ensuring it exists AND is owned by the user.
     * 
     * This is a common pattern - extract it to avoid duplication.
     * 
     * Security note: We use a single query that checks both ID and user ownership.
     * This prevents timing attacks that could reveal if an ID exists.
     */
    private Category findCategoryOwnedByUser(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
            .orElseThrow(() -> {
                log.debug("Category {} not found for user {}", categoryId, userId);
                return new ResourceNotFoundException("Category", categoryId);
            });
    }
}
```

---

## Layer 5: Controller

```java
package com.yourapp.controller;

import com.yourapp.dto.request.CreateCategoryRequest;
import com.yourapp.dto.request.UpdateCategoryRequest;
import com.yourapp.dto.response.CategoryResponse;
import com.yourapp.security.UserDetailsImpl;
import com.yourapp.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST Controller for Category operations.
 * 
 * Exception handling approach:
 * - Controller does NOT catch exceptions (with rare exceptions for special cases)
 * - All exceptions bubble up to GlobalExceptionHandler
 * - @Valid triggers validation BEFORE method executes
 * 
 * The controller's job:
 * 1. Extract data from HTTP request
 * 2. Get authenticated user
 * 3. Delegate to service
 * 4. Build HTTP response
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Categories", description = "Category management operations")
public class CategoryController {
    
    private final CategoryService categoryService;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a new category.
     * 
     * Exception flow:
     * 1. If @Valid fails → MethodArgumentNotValidException → 400
     * 2. If duplicate name → DuplicateResourceException → 409
     * 3. Any other error → Exception → 500
     */
    @Operation(summary = "Create a new category")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Category created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "409", description = "Category name already exists"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateCategoryRequest request) {
        
        log.info("POST /api/categories - Creating category: {}", request.name());
        
        Long userId = userDetails.getUser().getId();
        CategoryResponse response = categoryService.createCategory(userId, request);
        
        // Build Location header for created resource
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        
        return ResponseEntity.created(location).body(response);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Gets all categories for the authenticated user.
     * 
     * Exception flow:
     * - No exceptions expected (empty list is valid)
     */
    @Operation(summary = "Get all categories")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categories retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        
        log.debug("GET /api/categories - includeArchived: {}", includeArchived);
        
        Long userId = userDetails.getUser().getId();
        List<CategoryResponse> categories = categoryService.getAllCategories(userId, includeArchived);
        
        return ResponseEntity.ok(categories);
    }
    
    /**
     * Gets a specific category by ID.
     * 
     * Exception flow:
     * 1. If not found or not owned → ResourceNotFoundException → 404
     */
    @Operation(summary = "Get a category by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category found"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        
        log.debug("GET /api/categories/{}", id);
        
        Long userId = userDetails.getUser().getId();
        CategoryResponse response = categoryService.getCategoryById(id, userId);
        
        return ResponseEntity.ok(response);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Updates a category.
     * 
     * Exception flow:
     * 1. If @Valid fails → MethodArgumentNotValidException → 400
     * 2. If not found → ResourceNotFoundException → 404
     * 3. If duplicate name → DuplicateResourceException → 409
     */
    @Operation(summary = "Update a category")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "409", description = "Category name already exists"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        
        log.info("PUT /api/categories/{}", id);
        
        Long userId = userDetails.getUser().getId();
        CategoryResponse response = categoryService.updateCategory(id, userId, request);
        
        return ResponseEntity.ok(response);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Deletes a category.
     * 
     * Exception flow:
     * 1. If not found → ResourceNotFoundException → 404
     * 2. If has items → BusinessValidationException → 400
     */
    @Operation(summary = "Delete a category")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Category deleted"),
        @ApiResponse(responseCode = "400", description = "Cannot delete - category has items"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        
        log.info("DELETE /api/categories/{}", id);
        
        Long userId = userDetails.getUser().getId();
        categoryService.deleteCategory(id, userId);
        
        return ResponseEntity.noContent().build();
    }
}
```

---

## Layer 6: Exception Classes

```java
package com.yourapp.exception;

import lombok.Getter;

/**
 * Base exception for all business-related errors.
 */
@Getter
public abstract class BusinessException extends RuntimeException {
    
    private final String errorCode;
    
    protected BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected BusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
```

```java
package com.yourapp.exception;

/**
 * Thrown when a requested resource cannot be found.
 * Maps to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }
    
    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(
            String.format("%s not found with id: %s", resourceType, identifier),
            "RESOURCE_NOT_FOUND"
        );
    }
}
```

```java
package com.yourapp.exception;

/**
 * Thrown when attempting to create a duplicate resource.
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends BusinessException {
    
    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE");
    }
    
    public DuplicateResourceException(String resourceType, String field, Object value) {
        super(
            String.format("%s with %s '%s' already exists", resourceType, field, value),
            "DUPLICATE_RESOURCE"
        );
    }
}
```

```java
package com.yourapp.exception;

/**
 * Thrown when a business rule is violated.
 * Maps to HTTP 400 Bad Request.
 */
public class BusinessValidationException extends BusinessException {
    
    public BusinessValidationException(String message) {
        super(message, "BUSINESS_VALIDATION_FAILED");
    }
}
```

---

## Layer 7: Global Exception Handler

```java
package com.yourapp.exception.handler;

import com.yourapp.exception.*;
import com.yourapp.exception.dto.ErrorResponse;
import com.yourapp.exception.dto.ValidationErrorResponse;
import com.yourapp.exception.dto.ValidationErrorResponse.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Global exception handler - the safety net for all controllers.
 * 
 * Order of handlers matters! More specific exceptions first.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUSINESS EXCEPTIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        log.debug("Resource not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {
        
        log.warn("Duplicate resource: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidation(
            BusinessValidationException ex,
            HttpServletRequest request) {
        
        log.debug("Business validation failed: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDATION EXCEPTIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        log.debug("Validation failed with {} errors", ex.getErrorCount());
        
        List<FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldError(
                error.getField(),
                error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                error.getRejectedValue()
            ))
            .toList();
        
        ValidationErrorResponse error = ValidationErrorResponse.of(fieldErrors, request.getRequestURI());
        
        return ResponseEntity.badRequest().body(error);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CATCH-ALL
    // ═══════════════════════════════════════════════════════════════════════════
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            Exception ex,
            HttpServletRequest request) {
        
        // Log full stack trace for debugging
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        
        // Never expose internal details!
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## Layer 8: Response DTOs

```java
package com.yourapp.exception.dto;

import java.time.Instant;

/**
 * Standard error response format.
 */
public record ErrorResponse(
    int status,
    String errorCode,
    String message,
    String path,
    Instant timestamp
) {
    public static ErrorResponse of(int status, String errorCode, String message, String path) {
        return new ErrorResponse(status, errorCode, message, path, Instant.now());
    }
}
```

```java
package com.yourapp.exception.dto;

import java.time.Instant;
import java.util.List;

/**
 * Validation error response with field-level details.
 */
public record ValidationErrorResponse(
    int status,
    String errorCode,
    String message,
    String path,
    Instant timestamp,
    List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message, Object rejectedValue) {}
    
    public static ValidationErrorResponse of(List<FieldError> fieldErrors, String path) {
        return new ValidationErrorResponse(
            400,
            "VALIDATION_FAILED",
            String.format("Validation failed for %d field(s)", fieldErrors.size()),
            path,
            Instant.now(),
            fieldErrors
        );
    }
}
```

---

## Example Requests & Responses

### 1. Successful Creation

**Request:**
```http
POST /api/categories
Content-Type: application/json
Authorization: Bearer <jwt-token>

{
    "name": "Electronics",
    "description": "Electronic devices and gadgets"
}
```

**Response:** `201 Created`
```json
{
    "id": 1,
    "name": "Electronics",
    "description": "Electronic devices and gadgets",
    "archived": false,
    "itemCount": 0,
    "createdAt": "2025-01-20T10:30:00",
    "updatedAt": "2025-01-20T10:30:00"
}
```

### 2. Validation Error

**Request:**
```http
POST /api/categories
Content-Type: application/json
Authorization: Bearer <jwt-token>

{
    "name": "",
    "description": "This description is way too long..."
}
```

**Response:** `400 Bad Request`
```json
{
    "status": 400,
    "errorCode": "VALIDATION_FAILED",
    "message": "Validation failed for 1 field(s)",
    "path": "/api/categories",
    "timestamp": "2025-01-20T10:30:00Z",
    "fieldErrors": [
        {
            "field": "name",
            "message": "Category name is required",
            "rejectedValue": ""
        }
    ]
}
```

### 3. Duplicate Resource

**Request:**
```http
POST /api/categories
Content-Type: application/json
Authorization: Bearer <jwt-token>

{
    "name": "Electronics"
}
```

**Response:** `409 Conflict`
```json
{
    "status": 409,
    "errorCode": "DUPLICATE_RESOURCE",
    "message": "Category with name 'Electronics' already exists",
    "path": "/api/categories",
    "timestamp": "2025-01-20T10:30:00Z"
}
```

### 4. Resource Not Found

**Request:**
```http
GET /api/categories/999
Authorization: Bearer <jwt-token>
```

**Response:** `404 Not Found`
```json
{
    "status": 404,
    "errorCode": "RESOURCE_NOT_FOUND",
    "message": "Category not found with id: 999",
    "path": "/api/categories/999",
    "timestamp": "2025-01-20T10:30:00Z"
}
```

### 5. Business Rule Violation

**Request:**
```http
DELETE /api/categories/1
Authorization: Bearer <jwt-token>
```

**Response:** `400 Bad Request`
```json
{
    "status": 400,
    "errorCode": "BUSINESS_VALIDATION_FAILED",
    "message": "Cannot delete category 'Electronics' with 5 items. Archive it instead.",
    "path": "/api/categories/1",
    "timestamp": "2025-01-20T10:30:00Z"
}
```

---

## Testing the Exception Handling

```java
@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CategoryService categoryService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("POST /api/categories - validation error returns 400")
    void createCategory_ValidationError_Returns400() throws Exception {
        String invalidRequest = """
            { "name": "", "description": "Test" }
            """;
        
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }
    
    @Test
    @DisplayName("POST /api/categories - duplicate returns 409")
    void createCategory_Duplicate_Returns409() throws Exception {
        when(categoryService.createCategory(any(), any()))
            .thenThrow(new DuplicateResourceException("Category", "name", "Electronics"));
        
        String request = """
            { "name": "Electronics" }
            """;
        
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }
    
    @Test
    @DisplayName("GET /api/categories/{id} - not found returns 404")
    void getCategory_NotFound_Returns404() throws Exception {
        when(categoryService.getCategoryById(999L, any()))
            .thenThrow(new ResourceNotFoundException("Category", 999L));
        
        mockMvc.perform(get("/api/categories/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
    
    @Test
    @DisplayName("DELETE /api/categories/{id} - business rule violation returns 400")
    void deleteCategory_HasItems_Returns400() throws Exception {
        doThrow(new BusinessValidationException("Cannot delete category with items"))
            .when(categoryService).deleteCategory(1L, any());
        
        mockMvc.perform(delete("/api/categories/1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("BUSINESS_VALIDATION_FAILED"));
    }
}
```

---

## Summary: Exception Flow Quick Reference

| Scenario | Exception | HTTP Status | Error Code |
|----------|-----------|-------------|------------|
| Validation fails (@Valid) | MethodArgumentNotValidException | 400 | VALIDATION_FAILED |
| Entity not found | ResourceNotFoundException | 404 | RESOURCE_NOT_FOUND |
| Duplicate resource | DuplicateResourceException | 409 | DUPLICATE_RESOURCE |
| Business rule violated | BusinessValidationException | 400 | BUSINESS_VALIDATION_FAILED |
| Not authenticated | (Security filter) | 401 | AUTHENTICATION_REQUIRED |
| Not authorized | AccessDeniedException | 403 | ACCESS_DENIED |
| Unexpected error | Exception | 500 | INTERNAL_ERROR |

---

## Key Takeaways

1. **Controllers don't catch exceptions** - Let them bubble up
2. **Service layer throws specific exceptions** - With meaningful messages
3. **Repository returns Optional** - Service converts to exception
4. **GlobalExceptionHandler catches all** - Transforms to consistent response
5. **Never expose internals** - Generic message for 500 errors
6. **Log appropriately** - DEBUG for expected, ERROR for unexpected
