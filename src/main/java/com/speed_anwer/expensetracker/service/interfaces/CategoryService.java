package com.speed_anwer.expensetracker.service.interfaces;

import com.speed_anwer.expensetracker.dto.request.CategoryRequest;
import com.speed_anwer.expensetracker.dto.response.CategoryResponse;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request,Long userId);
}
