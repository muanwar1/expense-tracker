package com.speed_anwer.expensetracker.service.impl;

import com.speed_anwer.expensetracker.dto.request.CategoryRequest;
import com.speed_anwer.expensetracker.dto.response.CategoryResponse;
import com.speed_anwer.expensetracker.dto.response.PagedResponse;
import com.speed_anwer.expensetracker.entity.Category;
import com.speed_anwer.expensetracker.entity.User;
import com.speed_anwer.expensetracker.exception.ResourceConflictException;
import com.speed_anwer.expensetracker.exception.ResourceNotFoundException;
import com.speed_anwer.expensetracker.mapper.CategoryMapper;
import com.speed_anwer.expensetracker.repository.CategoryRepository;
import com.speed_anwer.expensetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryMapper categoryMapper;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, userRepository, categoryMapper);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    @Test
    void createCategory_rejectsDuplicateNameForSameUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(categoryRepository.findByNameAndUser(eq("Food"), any(User.class))).thenReturn(Optional.of(category(9L, "Food")));

        CategoryRequest request = new CategoryRequest();
        request.setName("Food");

        assertThatThrownBy(() -> categoryService.createCategory(request, 1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getCategoryById_returnsCategoryOwnedByUser() {
        User owner = user(1L);
        Category owned = category(5L, "Food");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findByIdAndUser(5L, owner)).thenReturn(Optional.of(owned));
        when(categoryMapper.toResponse(owned)).thenReturn(new CategoryResponse());

        assertThat(categoryService.getCategoryById(5L, 1L)).isNotNull();
    }

    @Test
    void getCategoryById_throwsWhenCategoryBelongsToAnotherUser() {
        User owner = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findByIdAndUser(5L, owner)).thenReturn(Optional.empty());

        // the classic swapped-parameters bug would make this lookup pass with the wrong ids
        assertThatThrownBy(() -> categoryService.getCategoryById(5L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCategory_rejectsConflictingName() {
        User owner = user(1L);
        Category existing = category(5L, "Old");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findByIdAndUser(5L, owner)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByNameAndUserAndIdNot("Food", owner, 5L))
                .thenReturn(Optional.of(category(8L, "Food")));

        CategoryRequest request = new CategoryRequest();
        request.setName("Food");

        assertThatThrownBy(() -> categoryService.updateCategory(5L, 1L, request))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void deleteCategory_removesOwnedCategory() {
        User owner = user(1L);
        Category owned = category(5L, "Food");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findByIdAndUser(5L, owner)).thenReturn(Optional.of(owned));

        categoryService.deleteCategory(5L, 1L);

        verify(categoryRepository).delete(owned);
    }

    @Test
    void getAllCategories_returnsPagedResponse() {
        User owner = user(1L);
        List<Category> categories = List.of(category(1L, "A"), category(2L, "B"));
        PageRequest pageRequest = PageRequest.of(0, 2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findByUser(owner, pageRequest)).thenReturn(new PageImpl<>(categories, pageRequest, 5));
        when(categoryMapper.toResponseList(any())).thenReturn(List.of(new CategoryResponse(), new CategoryResponse()));

        PagedResponse<CategoryResponse> result = categoryService.getAllCategories(1L, pageRequest);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }
}
