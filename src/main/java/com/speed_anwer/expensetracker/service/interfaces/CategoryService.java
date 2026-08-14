package com.speed_anwer.expensetracker.service.interfaces;

import com.speed_anwer.expensetracker.dto.request.CategoryRequest;
import com.speed_anwer.expensetracker.dto.response.CategoryResponse;
import com.speed_anwer.expensetracker.entity.User;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request, Long userId);

    List<CategoryResponse> getAllCategories(Long userId);

    CategoryResponse getCategoryById(Long CatrgoryId, Long userId);

    CategoryResponse updateCategory(Long CatrgoryId, Long userId , CategoryRequest request);

    void deleteCategory(Long CatrgoryId, Long userId);

}
