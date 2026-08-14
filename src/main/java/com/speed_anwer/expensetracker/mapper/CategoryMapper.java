package com.speed_anwer.expensetracker.mapper;

import com.speed_anwer.expensetracker.dto.request.CategoryRequest;
import com.speed_anwer.expensetracker.dto.response.CategoryResponse;
import com.speed_anwer.expensetracker.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toEntity(CategoryRequest request);
    @Mapping(source = "user.id" , target = "userId")
    CategoryResponse toResponse(Category category);
}