package com.speed_anwer.expensetracker.controller;

import com.speed_anwer.expensetracker.dto.request.ExpenseRequest;
import com.speed_anwer.expensetracker.dto.response.ExpenseResponse;
import com.speed_anwer.expensetracker.service.interfaces.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }
    @PostMapping("/{userId}/expense")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createEpense(@PathVariable Long userId, @Valid @RequestBody ExpenseRequest expenseRequest){
        return expenseService.createExpense(expenseRequest,userId) ;
    }

    @GetMapping("{userId}/expense")
    public List<ExpenseResponse> getAllExpense(@PathVariable Long userId) {
        return expenseService.getAllExpenses(userId);
    }

    @GetMapping("/{userId}/expense/{expenseId}")
    public ExpenseResponse getExpense(
            @PathVariable Long userId,
            @PathVariable Long expenseId) {

        return expenseService.getExpenseById(expenseId, userId);
    }

    @PutMapping("/{userId}/expense/{expenseId}")
    public ExpenseResponse updateExpense(
            @PathVariable Long userId,
            @PathVariable Long expenseId,
            @Valid @RequestBody ExpenseRequest expenseRequest) {

        return expenseService.updateExpense(expenseId, userId, expenseRequest);
    }

    @DeleteMapping("/{userId}/expense/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(
            @PathVariable Long userId,
            @PathVariable Long expenseId) {

        expenseService.deleteExpense(expenseId, userId);
    }


}