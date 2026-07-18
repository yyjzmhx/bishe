package com.example.personalmusicsystem.controller;

import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.User;
import com.example.personalmusicsystem.service.UserService;
import com.example.personalmusicsystem.service.storage.StorageService;
import com.example.personalmusicsystem.service.storage.impl.MinioStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private StorageService storageService;
    
    @Autowired
    private MinioStorageService minioStorageService;
    
    /**
     * 获取个人信息
     */
    @GetMapping("/profile")
    public Result<User> getProfile(@RequestAttribute Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 不返回密码
        user.setPassword(null);
        return Result.success(user);
    }
    
    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestBody User user,
                                       @RequestAttribute Long userId) {
        user.setId(userId);
        // 不允许修改密码和角色
        user.setPassword(null);
        user.setRole(null);
        
        boolean success = userService.update(user);
        if (success) {
            return Result.success("更新成功", null);
        }
        return Result.error("更新失败");
    }
    
    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                    @RequestAttribute Long userId) {
        try {
            // 验证文件类型
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只能上传图片文件");
            }
            
            // 验证文件大小（2MB）
            if (file.getSize() > 2 * 1024 * 1024) {
                return Result.error("图片大小不能超过 2MB");
            }
            
            // 上传到MinIO
            String relativePath = "avatars/" + minioStorageService.generateDatePath();
            String fileName = "avatar_" + userId + "_" + 
                             minioStorageService.generateUniqueFileName(file.getOriginalFilename());
            
            String fileUrl = storageService.saveFile(file, relativePath, fileName);
            
            // 更新用户头像URL
            User user = userService.getById(userId);
            if (user != null) {
                user.setAvatar(fileUrl);
                userService.update(user);
                log.info("用户头像更新成功，用户ID: {}, 头像URL: {}", userId, fileUrl);
            }
            
            Map<String, String> result = new HashMap<>();
            result.put("avatarUrl", fileUrl);
            result.put("message", "头像上传成功");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("头像上传失败，用户ID: {}", userId, e);
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<String> changePassword(@RequestBody Map<String, String> request,
                                        @RequestAttribute Long userId) {
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 验证旧密码
        User loginUser = userService.login(user.getUsername(), oldPassword);
        if (loginUser == null) {
            return Result.error("原密码错误");
        }
        
        // 更新密码
        user.setPassword(newPassword);
        boolean success = userService.update(user);
        if (success) {
            return Result.success("密码修改成功", null);
        }
        return Result.error("密码修改失败");
    }
}

