package com.speed_anwer.expensetracker.service.impl;

import com.speed_anwer.expensetracker.dto.request.ExpenseRequest;
import com.speed_anwer.expensetracker.dto.response.CategorySummary;
import com.speed_anwer.expensetracker.dto.response.ExpenseResponse;
import com.speed_anwer.expensetracker.dto.response.ExpenseStatistics;
import com.speed_anwer.expensetracker.entity.Category;
import com.speed_anwer.expensetracker.entity.Expense;
import com.speed_anwer.expensetracker.entity.User;
import com.speed_anwer.expensetracker.exception.ResourceNotFoundException;
import com.speed_anwer.expensetracker.mapper.ExpenseMapper;
import com.speed_anwer.expensetracker.repository.CategoryRepository;
import com.speed_anwer.expensetracker.repository.ExpenseRepository;
import com.speed_anwer.expensetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ExpenseMapper expenseMapper;

    private ExpenseServiceImpl expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseServiceImpl(userRepository, expenseRepository, categoryRepository, expenseMapper);
    }

    @Test
    void createExpense_linksUserAndCategory() {
        User owner = new User();
        owner.setId(1L);
        Category food = new Category();
        food.setId(2L);

        Expense mappedExpense = new Expense();
        Expense savedExpense = new Expense();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findByIdAndUser(2L, owner)).thenReturn(Optional.of(food));
        when(expenseMapper.toEntity(any(ExpenseRequest.class))).thenReturn(mappedExpense);
        when(expenseRepository.save(mappedExpense)).thenReturn(savedExpense);
        when(expenseMapper.toResponse(any(Expense.class))).thenReturn(new ExpenseResponse());

        ExpenseRequest request = new ExpenseRequest();
        request.setCategoryId(2L);
        expenseService.createExpense(request, 1L);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(owner);
        assertThat(captor.getValue().getCategory()).isEqualTo(food);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void createExpense_throwsWhenCategoryNotOwned() {
        User owner = new User();
        owner.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findByIdAndUser(99L, owner)).thenReturn(Optional.empty());

        ExpenseRequest request = new ExpenseRequest();
        request.setCategoryId(99L);

        assertThatThrownBy(() -> expenseService.createExpense(request, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void getExpenseById_cannotAccessAnotherUsersExpense() {
        User owner = new User();
        owner.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(expenseRepository.findByIdAndUser(5L, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(5L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStatistics_aggregatesTotalsAndBreakdown() {
        User owner = new User();
        owner.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(expenseRepository.aggregateTotals(owner, null, null))
                .thenReturn(List.<Object[]>of(new Object[]{new BigDecimal("675.00"), 5L, new BigDecimal("135.00")}));
        when(expenseRepository.summarizeByCategory(owner, null, null))
                .thenReturn(List.of(
                        new CategorySummary(1L, "Food", new BigDecimal("400.00"), 3),
                        new CategorySummary(2L, "Transport", new BigDecimal("275.00"), 2)
                ));

        ExpenseStatistics stats = expenseService.getStatistics(1L, null, null);

        assertThat(stats.getTotalAmount()).isEqualByComparingTo("675.00");
        assertThat(stats.getAverageAmount()).isEqualByComparingTo("135.00");
        assertThat(stats.getExpenseCount()).isEqualTo(5);
        assertThat(stats.getByCategory()).hasSize(2);
        assertThat(stats.getByCategory().get(0).categoryName()).isEqualTo("Food");
    }

    @Test
    void getAllExpenses_passesFiltersDown() {
        User owner = new User();
        owner.setId(1L);
        PageRequest pageRequest = PageRequest.of(0, 10);
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(expenseRepository.findByUserWithFilters(owner, 2L, start, end, pageRequest))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(), pageRequest, 0));
        when(expenseMapper.toResponseList(any())).thenReturn(List.of());

        var result = expenseService.getAllExpenses(1L, 2L, start, end, pageRequest);

        assertThat(result.getTotalElements()).isZero();
    }
}
