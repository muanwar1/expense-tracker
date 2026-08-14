package com.speed_anwer.expensetracker.mapper;

import com.speed_anwer.expensetracker.dto.request.RegisterRequest;
import com.speed_anwer.expensetracker.dto.response.UserResponse;
import com.speed_anwer.expensetracker.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toentity(RegisterRequest request);
    UserResponse toResponse(User user);
}
