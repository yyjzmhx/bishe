package com.example.personalmusicsystem.dto.response;

import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String role;  // USER 或 ADMIN
    private String nickname;
    private String avatar;
}

