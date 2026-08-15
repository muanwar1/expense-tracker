package com.speed_anwer.expensetracker.service.impl;

import com.speed_anwer.expensetracker.dto.request.ExpenseRequest;
import com.speed_anwer.expensetracker.dto.response.ExpenseResponse;
import com.speed_anwer.expensetracker.entity.Category;
import com.speed_anwer.expensetracker.entity.Expense;
import com.speed_anwer.expensetracker.entity.User;
import com.speed_anwer.expensetracker.mapper.ExpenseMapper;
import com.speed_anwer.expensetracker.repository.CategoryRepository;
import com.speed_anwer.expensetracker.repository.ExpenseRepository;
import com.speed_anwer.expensetracker.repository.UserRepository;
import com.speed_anwer.expensetracker.service.interfaces.ExpenseService;
import org.springframework.stereotype.Service;

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
    public ExpenseResponse createExpense(ExpenseRequest request, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(), user).orElseThrow(() -> new RuntimeException("Category not found"));

        Expense expense = expenseMapper.toEntity(request);
        expense.setUser(user);
        expense.setCategory(category);
        expense.setCreatedAt(LocalDateTime.now());
        expenseRepository.save(expense);
        return expenseMapper.toResponse(expense);
    }

    @Override
    public List<ExpenseResponse> getAllExpenses(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<Expense> expenses = expenseRepository.findByUser(user);
        List<ExpenseResponse> expenseResponseList = expenseMapper.toResponseList(expenses);
        return expenseResponseList;
    }

    @Override
    public ExpenseResponse getExpenseById(Long expenseId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Expense expense = expenseRepository.findByIdAndUser(expenseId, user).orElseThrow(() -> new RuntimeException("Expense not found"));
        return expenseMapper.toResponse(expense);
    }

    @Override
    public ExpenseResponse updateExpense(
            Long expenseId,
            Long userId,
            ExpenseRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Expense expense = expenseRepository.findByIdAndUser(expenseId, user)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        Category category = categoryRepository
                .findByIdAndUser(request.getCategoryId(), user)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setCategory(category);

        Expense updatedExpense = expenseRepository.save(expense);

        return expenseMapper.toResponse(updatedExpense);
    }

    @Override
    public void deleteExpense(Long expenseId, Long userId) {
        User user  = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Expense expense = expenseRepository.findByIdAndUser(expenseId,user).orElseThrow(() -> new RuntimeException("Expense not found")) ;
        expenseRepository.delete(expense);

    }
}