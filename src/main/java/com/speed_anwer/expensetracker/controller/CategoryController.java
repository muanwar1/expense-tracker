package com.speed_anwer.expensetracker.controller;

import com.speed_anwer.expensetracker.dto.request.CategoryRequest;
import com.speed_anwer.expensetracker.dto.response.CategoryResponse;
import com.speed_anwer.expensetracker.dto.response.PagedResponse;
import com.speed_anwer.expensetracker.security.UserPrincipal;
import com.speed_anwer.expensetracker.service.interfaces.CategoryService;
import com.speed_anwer.expensetracker.util.PageableUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name");

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CategoryRequest request) {

        return categoryService.createCategory(request, currentUser.getId());
    }

    @GetMapping
    public PagedResponse<CategoryResponse> getCategories(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Pageable pageable = PageableUtils.of(page, size, sortBy, sortDir, ALLOWED_SORT_FIELDS);
        return categoryService.getAllCategories(currentUser.getId(), pageable);
    }

    @GetMapping("/{categoryId}")
    public CategoryResponse getCategory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long categoryId) {

        return categoryService.getCategoryById(categoryId, currentUser.getId());
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse updateCategory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {

        return categoryService.updateCategory(categoryId, currentUser.getId(), request);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long categoryId) {

        categoryService.deleteCategory(categoryId, currentUser.getId());
    }
}
