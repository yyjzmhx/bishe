package com.example.personalmusicsystem.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置类
 */
@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioConfig {
    
    /**
     * MinIO服务器地址
     */
    private String endpoint;
    
    /**
     * 访问密钥ID
     */
    private String accessKey;
    
    /**
     * 访问密钥Secret
     */
    private String secretKey;
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 文件访问URL前缀（如果MinIO配置了域名）
     */
    private String baseUrl;
    
    /**
     * 创建MinioClient Bean
     */
    @Bean
    public MinioClient minioClient() {
        if (endpoint == null || accessKey == null || secretKey == null) {
            throw new IllegalStateException("MinIO配置不完整，请检查application.yml中的minio配置");
        }
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}

