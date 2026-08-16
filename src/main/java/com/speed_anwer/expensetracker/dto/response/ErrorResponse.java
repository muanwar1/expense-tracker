package com.speed_anwer.expensetracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    Map<String,String> errors;
    private LocalDateTime timeStamp;
}
