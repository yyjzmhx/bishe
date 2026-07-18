package com.example.personalmusicsystem.controller;

import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.dto.request.LoginRequest;
import com.example.personalmusicsystem.dto.request.RegisterRequest;
import com.example.personalmusicsystem.dto.response.LoginResponse;
import com.example.personalmusicsystem.entity.User;
import com.example.personalmusicsystem.service.PlaylistService;
import com.example.personalmusicsystem.service.UserService;
import com.example.personalmusicsystem.service.VerificationCodeService;
import com.example.personalmusicsystem.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private PlaylistService playlistService;
    
    @Autowired
    private VerificationCodeService verificationCodeService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.getUsername(), request.getPassword());
            if (user == null) {
                return Result.error("用户名或密码错误");
            }
            
            if (user.getStatus() == 0) {
                return Result.error("账号已被禁用");
            }
            
            // 生成Token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            
            // 构建登录响应
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setRole(user.getRole());
            response.setNickname(user.getNickname());
            response.setAvatar(user.getAvatar());
            
            // 登录成功后，清除该用户邮箱的验证码（如果存在）
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                verificationCodeService.deleteCode(user.getEmail(), "RESET_PASSWORD");
            }
            
            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error("登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送注册验证码
     */
    @PostMapping("/register/send-code")
    public Result<String> sendRegisterCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return Result.error("邮箱地址不能为空");
            }
            
            // 检查邮箱格式
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return Result.error("邮箱格式不正确");
            }
            
            // 检查邮箱是否已被注册
            User existingUser = userService.getByEmail(email);
            if (existingUser != null) {
                return Result.error("该邮箱已被注册");
            }
            
            // 生成并发送验证码
            verificationCodeService.generateAndSendCode(email, "REGISTER");
            
            return Result.success("验证码已发送至您的邮箱，请查收", null);
        } catch (Exception e) {
            log.error("发送注册验证码失败", e);
            return Result.error("发送验证码失败: " + e.getMessage());
        }
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest request) {
        try {
            // 验证邮箱验证码
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return Result.error("邮箱不能为空");
            }
            
            String emailCode = request.getEmailCode();
            if (emailCode == null || emailCode.trim().isEmpty()) {
                return Result.error("请输入邮箱验证码");
            }
            
            // 验证验证码并删除（防止重复使用）
            boolean isValid = verificationCodeService.verifyAndDeleteCode(request.getEmail(), emailCode, "REGISTER");
            if (!isValid) {
                return Result.error("验证码错误或已过期");
            }
            
            User user = new User();
            BeanUtils.copyProperties(request, user);
            
            userService.register(user);
            
            log.info("用户注册成功，用户名: {}, 邮箱: {}", user.getUsername(), user.getEmail());
            return Result.success("注册成功", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("注册失败", e);
            return Result.error("注册失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public Result<Map<String, Object>> getCurrentUser(@RequestAttribute Long userId,
                                                      @RequestAttribute String username,
                                                      @RequestAttribute String role) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("username", username);
        userInfo.put("role", role);
        
        User user = userService.getById(userId);
        if (user != null) {
            userInfo.put("nickname", user.getNickname());
            userInfo.put("avatar", user.getAvatar());
        }
        
        return Result.success(userInfo);
    }
    
    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestAttribute Long userId) {
        try {
            // 清除用户的播放列表和播放状态数据
            playlistService.clearUserData(userId);
            log.info("用户退出登录，已清除播放数据，用户ID: {}", userId);
            return Result.success("退出登录成功");
        } catch (Exception e) {
            log.error("退出登录失败", e);
            return Result.error("退出登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 忘记密码 - 发送验证码
     */
    @PostMapping("/forgot-password")
    public Result<String> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return Result.error("邮箱地址不能为空");
            }
            
            // 检查邮箱是否已注册
            User user = userService.getByEmail(email);
            if (user == null) {
                // 为了安全，不暴露邮箱是否存在
                return Result.success("如果该邮箱已注册，验证码已发送", null);
            }
            
            // 生成并发送验证码
            verificationCodeService.generateAndSendCode(email, "RESET_PASSWORD");
            
            return Result.success("验证码已发送至您的邮箱，请查收", null);
        } catch (Exception e) {
            log.error("发送验证码失败", e);
            return Result.error("发送验证码失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证验证码
     */
    @PostMapping("/verify-code")
    public Result<Map<String, String>> verifyCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");
            
            if (email == null || code == null) {
                return Result.error("邮箱和验证码不能为空");
            }
            
            boolean isValid = verificationCodeService.verifyCode(email, code, "RESET_PASSWORD");
            if (!isValid) {
                return Result.error("验证码错误或已过期");
            }
            
            Map<String, String> result = new HashMap<>();
            result.put("email", email);
            result.put("code", code);
            
            return Result.success("验证码验证成功", result);
        } catch (Exception e) {
            log.error("验证验证码失败", e);
            return Result.error("验证验证码失败: " + e.getMessage());
        }
    }
    
    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");
            String password = request.get("password");
            
            if (email == null || code == null || password == null) {
                return Result.error("参数不完整");
            }
            
            if (password.length() < 6) {
                return Result.error("密码长度不能少于6位");
            }
            
            // 再次验证验证码并删除（防止重复使用）
            boolean isValid = verificationCodeService.verifyAndDeleteCode(email, code, "RESET_PASSWORD");
            if (!isValid) {
                return Result.error("验证码错误或已过期");
            }
            
            // 查找用户
            User user = userService.getByEmail(email);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 重置密码
            userService.updatePassword(user.getId(), password);
            
            log.info("密码重置成功，邮箱: {}", email);
            return Result.success("密码重置成功，请使用新密码登录", null);
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return Result.error("重置密码失败: " + e.getMessage());
        }
    }
}

