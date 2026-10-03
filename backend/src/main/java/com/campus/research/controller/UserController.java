package com.campus.research.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.research.common.ApiResponse;
import com.campus.research.dto.AuthDtos.UserProfile;
import com.campus.research.entity.User;
import com.campus.research.mapper.UserMapper;
import com.campus.research.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final AuthService authService;

    @GetMapping
    public ApiResponse<List<UserProfile>> list(@RequestParam(required = false) String keyword) {
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getStatus, 1)
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(User::getRealName, keyword)
                        .or().like(User::getUsername, keyword)
                        .or().like(User::getDepartment, keyword))
                .orderByAsc(User::getRealName));
        return ApiResponse.success(users.stream().map(authService::toProfile).toList());
    }
}
