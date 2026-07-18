package com.example.personalmusicsystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务（使用Redis存储）
 */
@Service
@Slf4j
public class VerificationCodeService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private EmailService emailService;
    
    private static final int CODE_LENGTH = 6;
    private static final long DEFAULT_EXPIRE_SECONDS = 60; // 默认1分钟过期（用于密码重置）
    private static final long REGISTER_EXPIRE_SECONDS = 600; // 注册验证码10分钟过期
    private static final String REDIS_KEY_PREFIX = "verification_code:";
    
    /**
     * 生成并发送验证码
     */
    public void generateAndSendCode(String email, String type) {
        // 生成6位数字验证码
        String code = generateCode();
        
        // 根据类型设置不同的过期时间
        long expireSeconds = "REGISTER".equals(type) ? REGISTER_EXPIRE_SECONDS : DEFAULT_EXPIRE_SECONDS;
        
        // 存储到Redis
        String redisKey = buildRedisKey(type, email);
        redisTemplate.opsForValue().set(redisKey, code, expireSeconds, TimeUnit.SECONDS);
        
        // 发送邮件
        emailService.sendVerificationCode(email, code, type);
        
        log.info("验证码生成成功，邮箱: {}, 类型: {}, 过期时间: {}秒", email, type, expireSeconds);
    }
    
    /**
     * 验证验证码（验证成功后不删除，允许重复验证）
     */
    public boolean verifyCode(String email, String code, String type) {
        String redisKey = buildRedisKey(type, email);
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        
        if (storedCode == null) {
            log.warn("验证码不存在或已过期，邮箱: {}, 验证码: {}", email, code);
            return false;
        }
        
        if (!storedCode.equals(code)) {
            log.warn("验证码错误，邮箱: {}, 输入的验证码: {}", email, code);
            return false;
        }
        
        log.info("验证码验证成功，邮箱: {}", email);
        return true;
    }
    
    /**
     * 验证并删除验证码（验证成功后删除，防止重复使用）
     */
    public boolean verifyAndDeleteCode(String email, String code, String type) {
        String redisKey = buildRedisKey(type, email);
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        
        if (storedCode == null) {
            log.warn("验证码不存在或已过期，邮箱: {}, 验证码: {}", email, code);
            return false;
        }
        
        if (!storedCode.equals(code)) {
            log.warn("验证码错误，邮箱: {}, 输入的验证码: {}", email, code);
            return false;
        }
        
        // 验证成功后删除验证码（防止重复使用）
        redisTemplate.delete(redisKey);
        
        log.info("验证码验证成功并已删除，邮箱: {}", email);
        return true;
    }
    
    /**
     * 删除验证码（登录成功后调用）
     */
    public void deleteCode(String email, String type) {
        String redisKey = buildRedisKey(type, email);
        Boolean deleted = redisTemplate.delete(redisKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("验证码已删除，邮箱: {}, 类型: {}", email, type);
        }
    }
    
    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * 构建Redis Key
     */
    private String buildRedisKey(String type, String email) {
        return REDIS_KEY_PREFIX + type + ":" + email;
    }
}

