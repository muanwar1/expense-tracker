package com.speed_anwer.expensetracker.controller;

import com.speed_anwer.expensetracker.dto.request.CategoryRequest;
import com.speed_anwer.expensetracker.dto.request.RegisterRequest;
import com.speed_anwer.expensetracker.dto.response.CategoryResponse;
import com.speed_anwer.expensetracker.dto.response.UserResponse;
import com.speed_anwer.expensetracker.service.interfaces.CategoryService;
import com.speed_anwer.expensetracker.service.interfaces.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CategoryService categoryService;

    public UserController(UserService userService, CategoryService categoryService) {
        this.userService = userService;
        this.categoryService = categoryService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/{userId}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request, @PathVariable Long userId) {
        return categoryService.createCategory(request, userId);
    }

    @GetMapping("/{userId}/categories")
    public List<CategoryResponse> getCategories(@PathVariable Long userId) {
        return categoryService.getAllCategories(userId);
    }

    @GetMapping("/{userId}/categories/{categoryId}")
    public CategoryResponse getCategory(@PathVariable Long userId, @PathVariable Long categoryId) {
        return categoryService.getCategoryById(categoryId, userId);
    }

    @PutMapping("/{userId}/categories/{categoryId}")
    public CategoryResponse updatecategory(@PathVariable Long userId, @PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(categoryId, userId, request);
    }

    @DeleteMapping("/{userId}/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            @PathVariable Long userId,
            @PathVariable Long categoryId) {

        categoryService.deleteCategory(categoryId, userId);
    }
}