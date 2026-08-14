package com.speed_anwer.expensetracker.service.impl;

import com.speed_anwer.expensetracker.dto.request.RegisterRequest;
import com.speed_anwer.expensetracker.dto.response.UserResponse;
import com.speed_anwer.expensetracker.entity.User;
import com.speed_anwer.expensetracker.mapper.UserMapper;
import com.speed_anwer.expensetracker.repository.UserRepository;
import com.speed_anwer.expensetracker.service.interfaces.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public  UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if(userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw  new RuntimeException("Email already exists");
        }
        User user = userMapper.toentity(request);
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }
}
