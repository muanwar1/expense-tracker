package com.speed_anwer.expensetracker.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
public class ExpenseRequest {
    @NotBlank
    private String title;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    @PastOrPresent
    private LocalDate expenseDate;

    @NotNull
    private Long categoryId;
}
