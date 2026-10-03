package com.campus.research.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {
    }

    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空")
            @Size(min = 3, max = 30, message = "用户名长度应为 3-30 个字符") String username,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 50, message = "密码长度应为 6-50 个字符") String password,
            @NotBlank(message = "姓名不能为空") String realName,
            @Email(message = "邮箱格式不正确") String email,
            @NotBlank(message = "所属院系不能为空") String department,
            @NotBlank(message = "身份不能为空") String identity) {
    }

    public record LoginResponse(String token, UserProfile user) {
    }

    public record UserProfile(
            Long id,
            String username,
            String realName,
            String email,
            String phone,
            String avatar,
            String department,
            String identity,
            String role) {
    }
}
