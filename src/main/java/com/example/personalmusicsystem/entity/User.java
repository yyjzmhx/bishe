package com.example.personalmusicsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
public class User {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String password;
    private String avatar;
    private String nickname;
    private String preferences;  // JSON字符串
    private String role;  // USER 或 ADMIN
    private Integer status;  // 1-正常 0-禁用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

