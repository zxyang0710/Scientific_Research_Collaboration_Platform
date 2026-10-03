package com.campus.research.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.research.common.BusinessException;
import com.campus.research.dto.AuthDtos.LoginRequest;
import com.campus.research.dto.AuthDtos.LoginResponse;
import com.campus.research.dto.AuthDtos.RegisterRequest;
import com.campus.research.dto.AuthDtos.UserProfile;
import com.campus.research.entity.User;
import com.campus.research.mapper.UserMapper;
import com.campus.research.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被停用");
        }
        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, toProfile(user));
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        long count = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.username()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName());
        user.setEmail(request.email());
        user.setDepartment(request.department());
        user.setTitle(request.title());
        user.setRole("RESEARCHER");
        user.setStatus(1);
        userMapper.insert(user);
        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, toProfile(user));
    }

    public UserProfile toProfile(User user) {
        return new UserProfile(user.getId(), user.getUsername(), user.getRealName(), user.getEmail(),
                user.getPhone(), user.getAvatar(), user.getDepartment(), user.getTitle(), user.getRole());
    }
}
