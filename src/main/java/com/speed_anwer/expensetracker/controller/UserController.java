package com.speed_anwer.expensetracker.controller;

import com.speed_anwer.expensetracker.dto.response.UserResponse;
import com.speed_anwer.expensetracker.security.UserPrincipal;
import com.speed_anwer.expensetracker.service.interfaces.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        return userService.getUserById(currentUser.getId());
    }
}
