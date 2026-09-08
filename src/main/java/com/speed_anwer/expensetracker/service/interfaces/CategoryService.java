package com.speed_anwer.expensetracker.service.interfaces;

import com.speed_anwer.expensetracker.dto.request.CategoryRequest;
import com.speed_anwer.expensetracker.dto.response.CategoryResponse;
import com.speed_anwer.expensetracker.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request, Long userId);

    PagedResponse<CategoryResponse> getAllCategories(Long userId, Pageable pageable);

    CategoryResponse getCategoryById(Long CatrgoryId, Long userId);

    CategoryResponse updateCategory(Long CatrgoryId, Long userId , CategoryRequest request);

    void deleteCategory(Long CatrgoryId, Long userId);

}
