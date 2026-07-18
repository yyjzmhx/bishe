package com.example.personalmusicsystem.config;

import com.alibaba.fastjson2.JSON;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 */
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用String序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // 使用JSON序列化器
        RedisSerializer<Object> jsonSerializer = new RedisSerializer<Object>() {
            @Override
            public byte[] serialize(Object o) {
                if (o == null) {
                    return new byte[0];
                }
                return JSON.toJSONString(o).getBytes();
            }
            
            @Override
            public Object deserialize(byte[] bytes) {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                return JSON.parseObject(new String(bytes), Object.class);
            }
        };
        
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}

