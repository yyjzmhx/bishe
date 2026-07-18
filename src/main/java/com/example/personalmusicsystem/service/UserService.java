package com.example.personalmusicsystem.service;

import com.example.personalmusicsystem.entity.User;
import com.example.personalmusicsystem.mapper.UserMapper;
import com.example.personalmusicsystem.util.BCryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户服务类
 */
@Service
@Slf4j
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private BCryptUtil bCryptUtil;
    
    /**
     * 用户登录
     */
    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null;
        }
        
        if (!bCryptUtil.matches(password, user.getPassword())) {
            return null;
        }
        
        return user;
    }
    
    /**
     * 用户注册
     */
    public boolean register(User user) {
        // 检查用户名是否已存在
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查手机号是否已存在
        if (user.getPhone() != null && userMapper.selectByPhone(user.getPhone()) != null) {
            throw new RuntimeException("手机号已被注册");
        }
        
        // 检查邮箱是否已存在
        if (user.getEmail() != null && userMapper.selectByEmail(user.getEmail()) != null) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 加密密码
        user.setPassword(bCryptUtil.encode(user.getPassword()));
        
        // 设置默认角色
        if (user.getRole() == null) {
            user.setRole("USER");
        }
        
        // 设置默认状态
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        
        return userMapper.insert(user) > 0;
    }
    
    /**
     * 根据ID查询用户
     */
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
    
    /**
     * 更新用户信息
     */
    public boolean update(User user) {
        return userMapper.update(user) > 0;
    }
    
    /**
     * 根据邮箱查询用户
     */
    public User getByEmail(String email) {
        return userMapper.selectByEmail(email);
    }
    
    /**
     * 更新密码
     */
    public boolean updatePassword(Long userId, String newPassword) {
        User user = new User();
        user.setId(userId);
        user.setPassword(bCryptUtil.encode(newPassword));
        return userMapper.update(user) > 0;
    }
}

