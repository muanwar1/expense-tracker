package com.speed_anwer.expensetracker.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExpenseResponse {

    private Long id;
    private String title;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
    private Long categoryId;
}