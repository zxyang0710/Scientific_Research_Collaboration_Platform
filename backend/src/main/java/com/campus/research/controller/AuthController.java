package com.campus.research.controller;

import com.campus.research.common.ApiResponse;
import com.campus.research.dto.AuthDtos.LoginRequest;
import com.campus.research.dto.AuthDtos.LoginResponse;
import com.campus.research.dto.AuthDtos.RegisterRequest;
import com.campus.research.dto.AuthDtos.UserProfile;
import com.campus.research.entity.User;
import com.campus.research.mapper.UserMapper;
import com.campus.research.security.SecurityUtils;
import com.campus.research.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfile> me() {
        User user = userMapper.selectById(SecurityUtils.currentUserId());
        return ApiResponse.success(authService.toProfile(user));
    }
}
