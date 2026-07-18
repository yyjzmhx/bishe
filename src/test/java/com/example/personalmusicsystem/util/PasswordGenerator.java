package com.example.personalmusicsystem.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具（用于生成测试数据的BCrypt密码）
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("=== BCrypt密码哈希值 ===");
        System.out.println("admin123: " + encoder.encode("admin123"));
        System.out.println("user123: " + encoder.encode("user123"));
    }
}

