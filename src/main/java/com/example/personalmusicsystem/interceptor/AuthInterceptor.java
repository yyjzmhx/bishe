package com.example.personalmusicsystem.interceptor;

import com.example.personalmusicsystem.annotation.RequireAdmin;
import com.example.personalmusicsystem.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

/**
 * 认证拦截器
 */
@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    // 不需要认证的路径
    private static final String[] EXCLUDE_PATHS = {
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/captcha",
        "/api/files"
    };
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        
        String path = request.getRequestURI();
        
        // 排除登录、注册等公开接口
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.startsWith(excludePath)) {
                return true;
            }
        }
        
        // 获取Token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            sendErrorResponse(response, 401, "未登录，请先登录");
            return false;
        }
        
        // 移除 "Bearer " 前缀（如果有）
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            sendErrorResponse(response, 401, "Token无效或已过期");
            return false;
        }
        
        // 从Token中获取角色
        String role = jwtUtil.getRoleFromToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        // 将用户信息存储到request中，方便后续使用
        request.setAttribute("userId", userId);
        request.setAttribute("username", jwtUtil.getUsernameFromToken(token));
        request.setAttribute("role", role);
        
        // 检查是否需要管理员权限
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
            
            // 如果方法或类上有@RequireAdmin注解，检查角色
            if (requireAdmin == null) {
                requireAdmin = handlerMethod.getBeanType().getAnnotation(RequireAdmin.class);
            }
            
            if (requireAdmin != null && !"ADMIN".equals(role)) {
                sendErrorResponse(response, 403, "权限不足，需要管理员权限");
                return false;
            }
        }
        
        // 路径级别的权限检查：/api/admin/** 需要管理员权限
        if (path.startsWith("/api/admin/") && !"ADMIN".equals(role)) {
            sendErrorResponse(response, 403, "权限不足，需要管理员权限");
            return false;
        }
        
        return true;
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        
        // 手动构建JSON字符串，避免依赖ObjectMapper
        String json = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}", 
            code, escapeJson(message));
        
        writer.write(json);
        writer.flush();
        writer.close();
    }
    
    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

