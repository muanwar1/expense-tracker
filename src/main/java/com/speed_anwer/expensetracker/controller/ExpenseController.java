package com.speed_anwer.expensetracker.controller;

import com.speed_anwer.expensetracker.dto.request.ExpenseRequest;
import com.speed_anwer.expensetracker.dto.response.ExpenseResponse;
import com.speed_anwer.expensetracker.dto.response.ExpenseStatistics;
import com.speed_anwer.expensetracker.dto.response.PagedResponse;
import com.speed_anwer.expensetracker.security.UserPrincipal;
import com.speed_anwer.expensetracker.service.interfaces.ExpenseService;
import com.speed_anwer.expensetracker.util.PageableUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "title", "amount", "expenseDate", "createdAt");

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ExpenseRequest expenseRequest) {

        return expenseService.createExpense(expenseRequest, currentUser.getId());
    }

    @GetMapping
    public PagedResponse<ExpenseResponse> getAllExpenses(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "expenseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PageableUtils.of(page, size, sortBy, sortDir, ALLOWED_SORT_FIELDS);
        return expenseService.getAllExpenses(currentUser.getId(), categoryId, startDate, endDate, pageable);
    }

    @GetMapping("/statistics")
    public ExpenseStatistics getStatistics(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return expenseService.getStatistics(currentUser.getId(), startDate, endDate);
    }

    @GetMapping("/{expenseId}")
    public ExpenseResponse getExpense(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long expenseId) {

        return expenseService.getExpenseById(expenseId, currentUser.getId());
    }

    @PutMapping("/{expenseId}")
    public ExpenseResponse updateExpense(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long expenseId,
            @Valid @RequestBody ExpenseRequest expenseRequest) {

        return expenseService.updateExpense(expenseId, currentUser.getId(), expenseRequest);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long expenseId) {

        expenseService.deleteExpense(expenseId, currentUser.getId());
    }
}
