package com.example.personalmusicsystem.controller.admin;

import com.example.personalmusicsystem.annotation.RequireAdmin;
import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.User;
import com.example.personalmusicsystem.mapper.UserMapper;
import com.example.personalmusicsystem.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员用户管理控制器
 */
@RestController
@RequestMapping("/api/admin/users")
@RequireAdmin
@Slf4j
public class AdminUserController {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取用户列表
     */
    @GetMapping
    public Result<Map<String, Object>> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        int offset = (pageNum - 1) * pageSize;
        List<User> list = userMapper.selectList(keyword, role, status, offset, pageSize);
        long total = userMapper.count(keyword, role, status);
        
        // 移除密码
        list.forEach(user -> user.setPassword(null));
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public Result<User> getUserDetail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public Result<String> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        // 不允许修改密码
        user.setPassword(null);
        
        boolean success = userService.update(user);
        if (success) {
            return Result.success("更新成功", null);
        }
        return Result.error("更新失败");
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        boolean success = userMapper.deleteById(id) > 0;
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }
    
    /**
     * 启用/禁用用户
     */
    @PutMapping("/{id}/status")
    public Result<String> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Integer status = request.get("status");
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        
        boolean success = userService.update(user);
        if (success) {
            return Result.success("状态更新成功", null);
        }
        return Result.error("状态更新失败");
    }
}

