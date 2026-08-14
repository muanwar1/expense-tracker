package com.speed_anwer.expensetracker.dto.response;

import lombok.Data;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private Long userId;
}
