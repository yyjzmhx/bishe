package com.example.personalmusicsystem.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient 配置
 * 使用 Spring AI Alibaba DashScope 自动配置的 ChatModel
 */
@Configuration
public class ChatClientConfig {
    
    /**
     * 创建 ChatClient Bean
     * Spring AI Alibaba Starter 会自动配置 DashScopeChatModel
     * 我们只需要注入并使用它创建 ChatClient
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .build();
    }
}

