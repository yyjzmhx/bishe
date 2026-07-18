package com.example.personalmusicsystem.dto.request;

import lombok.Data;

/**
 * 注册请求DTO
 */
@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String phone;
    private String email;
    private String emailCode;  // 邮箱验证码
    private String nickname;
}

