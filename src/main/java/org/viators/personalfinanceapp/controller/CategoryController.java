package org.viators.personalfinanceapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.viators.personalfinanceapp.dto.category.request.CreateCategoryRequest;
import org.viators.personalfinanceapp.dto.category.request.UpdateCategoryRequest;
import org.viators.personalfinanceapp.dto.category.response.CategoryDetailsResponse;
import org.viators.personalfinanceapp.dto.category.response.CategorySummaryResponse;
import org.viators.personalfinanceapp.service.CategoryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<Page<CategorySummaryResponse>> getCategories(@AuthenticationPrincipal(expression = "currentUser.uuid") String userUuid,
                                                                       @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<CategorySummaryResponse> response = categoryService.getCategories(userUuid, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<CategoryDetailsResponse> getCategoryWithDetails(@AuthenticationPrincipal(expression = "currentUser.uuid") String userUuid,
                                                                          @PathVariable("uuid") String categoryUuid) {

        CategoryDetailsResponse response = categoryService.getCategoryWithDetails(userUuid, categoryUuid);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/create")
    public ResponseEntity<CategorySummaryResponse> createCategory(@AuthenticationPrincipal(expression = "currentUser.uuid") String userUuid,
                                                                  @RequestBody @Valid CreateCategoryRequest request) {

        CategorySummaryResponse response = categoryService.create(userUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{categoryUuid}")
    public ResponseEntity<CategorySummaryResponse> updateCategory(@AuthenticationPrincipal(expression = "currentUser.uuid") String userUuid,
                                                                  @PathVariable String categoryUuid,
                                                                  @RequestBody @Valid UpdateCategoryRequest request) {

        CategorySummaryResponse response = categoryService.update(userUuid, categoryUuid, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{categoryUuid}/items/{itemUuid}")
    public ResponseEntity<CategoryDetailsResponse> addItem(@AuthenticationPrincipal(expression = "currentUser.uuid") String userUuid,
                                                           @PathVariable String categoryUuid,
                                                           @PathVariable String itemUuid) {

        CategoryDetailsResponse response = categoryService.addItem(userUuid, categoryUuid, itemUuid);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{categoryUuid}/items/{itemUuid}")
    public ResponseEntity<CategoryDetailsResponse> removeItem(@AuthenticationPrincipal(expression = "currentUser.uuid") String userUuid,
                                                              @PathVariable String categoryUuid,
                                                              @PathVariable String itemUuid) {

        CategoryDetailsResponse response = categoryService.removeItem(userUuid, categoryUuid, itemUuid);
        return ResponseEntity.ok(response);
    }
}
