package com.example.personalmusicsystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * 邮件发送服务
 */
@Service
@Slf4j
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    /**
     * 发送验证码邮件
     */
    public void sendVerificationCode(String toEmail, String code, String type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "MusicAI 个性化音乐推荐系统");
            helper.setTo(toEmail);
            
            // 根据类型设置不同的邮件主题和内容
            if ("REGISTER".equals(type)) {
                helper.setSubject("【MusicAI】注册验证码");
            } else {
                helper.setSubject("【MusicAI】密码重置验证码");
            }
            
            // HTML邮件内容
            String htmlContent = buildVerificationCodeEmail(code, type);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("验证码邮件发送成功，收件人: {}, 类型: {}", toEmail, type);
        } catch (Exception e) {
            log.error("发送验证码邮件失败，收件人: {}", toEmail, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建验证码邮件HTML内容
     */
    private String buildVerificationCodeEmail(String code, String type) {
        String title = "REGISTER".equals(type) ? "🎵 MusicAI 账号注册" : "🎵 MusicAI 密码重置";
        String action = "REGISTER".equals(type) ? "注册账号" : "重置密码";
        String expireTime = "REGISTER".equals(type) ? "10分钟" : "1分钟";
        String warning = "REGISTER".equals(type) 
            ? "⚠️ 如果您没有申请注册账号，请忽略此邮件。" 
            : "⚠️ 如果您没有申请重置密码，请忽略此邮件。";
        
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: 'Microsoft YaHei', Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".header h1 { color: white; margin: 0; font-size: 24px; }" +
                ".content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }" +
                ".code-box { background: white; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }" +
                ".code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 8px; }" +
                ".footer { text-align: center; margin-top: 30px; color: #718096; font-size: 12px; }" +
                ".warning { color: #ef4444; font-size: 14px; margin-top: 20px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>" + title + "</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>您好，</p>" +
                "<p>您正在申请" + action + "，验证码如下：</p>" +
                "<div class='code-box'>" +
                "<div class='code'>" + code + "</div>" +
                "</div>" +
                "<p>验证码有效期为 <strong>" + expireTime + "</strong>，请尽快使用。</p>" +
                "<p class='warning'>" + warning + "</p>" +
                "<p>此邮件由系统自动发送，请勿回复。</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2024 MusicAI 个性化音乐推荐系统. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}

