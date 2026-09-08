package com.speed_anwer.expensetracker.dto.request;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}
