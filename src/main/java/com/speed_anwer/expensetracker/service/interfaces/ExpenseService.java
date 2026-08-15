package com.speed_anwer.expensetracker.service.interfaces;

import com.speed_anwer.expensetracker.dto.request.ExpenseRequest;
import com.speed_anwer.expensetracker.dto.response.ExpenseResponse;

import java.util.List;

public interface ExpenseService {

    ExpenseResponse createExpense(ExpenseRequest request, Long userId);

    List<ExpenseResponse> getAllExpenses(Long userId);

    ExpenseResponse getExpenseById(Long expenseId, Long userId);

    ExpenseResponse updateExpense(
            Long expenseId,
            Long userId,
            ExpenseRequest request
    );

    void deleteExpense(Long expenseId, Long userId);
}