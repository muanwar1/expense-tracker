package com.speed_anwer.expensetracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class ExpenseStatistics {
    private BigDecimal totalAmount;
    private BigDecimal averageAmount;
    private long expenseCount;
    private List<CategorySummary> byCategory;
}
