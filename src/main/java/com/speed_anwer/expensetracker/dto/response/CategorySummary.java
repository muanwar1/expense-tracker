package com.speed_anwer.expensetracker.dto.response;

import java.math.BigDecimal;

public record CategorySummary(
        Long categoryId,
        String categoryName,
        BigDecimal totalAmount,
        long expenseCount
) {
}
