package com.speed_anwer.expensetracker.service.interfaces;

import com.speed_anwer.expensetracker.dto.request.RegisterRequest;
import com.speed_anwer.expensetracker.dto.response.UserResponse;

public interface UserService {
    UserResponse register(RegisterRequest request);

    UserResponse getUserById(Long userId);

    UserResponse getUserByEmail(String email);
}
