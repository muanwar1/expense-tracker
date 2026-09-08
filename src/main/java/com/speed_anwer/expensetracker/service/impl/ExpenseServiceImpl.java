package com.speed_anwer.expensetracker.service.impl;

import com.speed_anwer.expensetracker.dto.request.ExpenseRequest;
import com.speed_anwer.expensetracker.dto.response.CategorySummary;
import com.speed_anwer.expensetracker.dto.response.ExpenseResponse;
import com.speed_anwer.expensetracker.dto.response.ExpenseStatistics;
import com.speed_anwer.expensetracker.dto.response.PagedResponse;
import com.speed_anwer.expensetracker.entity.Category;
import com.speed_anwer.expensetracker.entity.Expense;
import com.speed_anwer.expensetracker.entity.User;
import com.speed_anwer.expensetracker.exception.ResourceNotFoundException;
import com.speed_anwer.expensetracker.mapper.ExpenseMapper;
import com.speed_anwer.expensetracker.repository.CategoryRepository;
import com.speed_anwer.expensetracker.repository.ExpenseRepository;
import com.speed_anwer.expensetracker.repository.UserRepository;
import com.speed_anwer.expensetracker.service.interfaces.ExpenseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class ExpenseServiceImpl implements ExpenseService {
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseMapper expenseMapper;

    public ExpenseServiceImpl(UserRepository userRepository, ExpenseRepository expenseRepository,
                              CategoryRepository categoryRepository, ExpenseMapper expenseMapper) {
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.expenseMapper = expenseMapper;
    }

    @Override
    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(), user).orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Expense expense = expenseMapper.toEntity(request);
        expense.setUser(user);
        expense.setCategory(category);
        expense.setCreatedAt(LocalDateTime.now());
        expenseRepository.save(expense);
        return expenseMapper.toResponse(expense);
    }

    @Override
    public PagedResponse<ExpenseResponse> getAllExpenses(
            Long userId,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Page<Expense> page = expenseRepository.findByUserWithFilters(user, categoryId, startDate, endDate, pageable);

        return new PagedResponse<>(
                expenseMapper.toResponseList(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Override
    public ExpenseStatistics getStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Object[]> result = expenseRepository.aggregateTotals(user, startDate, endDate);
        if (result.isEmpty()) {
            return new ExpenseStatistics(BigDecimal.ZERO, BigDecimal.ZERO, 0L, List.of());
        }

        Object[] totals = result.get(0);
        BigDecimal totalAmount = new BigDecimal(totals[0].toString());
        long count = ((Number) totals[1]).longValue();
        BigDecimal averageAmount = new BigDecimal(totals[2].toString());

        List<CategorySummary> byCategory = expenseRepository.summarizeByCategory(user, startDate, endDate);

        return new ExpenseStatistics(totalAmount, averageAmount, count, byCategory);
    }

    @Override
    public ExpenseResponse getExpenseById(Long expenseId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Expense expense = expenseRepository.findByIdAndUser(expenseId, user).orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return expenseMapper.toResponse(expense);
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(
            Long expenseId,
            Long userId,
            ExpenseRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Expense expense = expenseRepository.findByIdAndUser(expenseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        Category category = categoryRepository
                .findByIdAndUser(request.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setCategory(category);

        Expense updatedExpense = expenseRepository.save(expense);

        return expenseMapper.toResponse(updatedExpense);
    }

    @Override
    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {
        User user  = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Expense expense = expenseRepository.findByIdAndUser(expenseId,user).orElseThrow(() -> new ResourceNotFoundException("Expense not found")) ;
        expenseRepository.delete(expense);

    }
}