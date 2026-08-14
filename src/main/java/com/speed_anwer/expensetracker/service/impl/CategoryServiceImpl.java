package com.speed_anwer.expensetracker.service.impl;

import com.speed_anwer.expensetracker.dto.request.CategoryRequest;
import com.speed_anwer.expensetracker.dto.response.CategoryResponse;
import com.speed_anwer.expensetracker.entity.Category;
import com.speed_anwer.expensetracker.entity.User;
import com.speed_anwer.expensetracker.mapper.CategoryMapper;
import com.speed_anwer.expensetracker.repository.CategoryRepository;
import com.speed_anwer.expensetracker.repository.UserRepository;
import com.speed_anwer.expensetracker.service.interfaces.CategoryService;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               UserRepository userRepository,
                               CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.categoryMapper = categoryMapper;
    }
    @Override
    public CategoryResponse createCategory(CategoryRequest request,Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if(categoryRepository.findByNameAndUser(request.getName(),user).isPresent()){
            throw new RuntimeException("Category already exists");
        }
        Category category = categoryMapper.toEntity(request);
        category.setUser(user);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }
}
