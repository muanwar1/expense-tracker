package com.speed_anwer.expensetracker.service.interfaces;

import com.speed_anwer.expensetracker.dto.request.RegisterRequest;
import com.speed_anwer.expensetracker.dto.response.UserResponse;
import com.speed_anwer.expensetracker.entity.User;

public interface UserService {
    UserResponse register(RegisterRequest request);
}
