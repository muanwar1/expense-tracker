package com.speed_anwer.expensetracker.service.interfaces;

import com.speed_anwer.expensetracker.dto.request.ExpenseRequest;
import com.speed_anwer.expensetracker.dto.response.ExpenseResponse;
import com.speed_anwer.expensetracker.dto.response.ExpenseStatistics;
import com.speed_anwer.expensetracker.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ExpenseService {

    ExpenseResponse createExpense(ExpenseRequest request, Long userId);

    PagedResponse<ExpenseResponse> getAllExpenses(
            Long userId,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    ExpenseStatistics getStatistics(Long userId, LocalDate startDate, LocalDate endDate);

    ExpenseResponse getExpenseById(Long expenseId, Long userId);

    ExpenseResponse updateExpense(
            Long expenseId,
            Long userId,
            ExpenseRequest request
    );

    void deleteExpense(Long expenseId, Long userId);
}
