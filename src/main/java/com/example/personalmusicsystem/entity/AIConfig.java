package com.example.personalmusicsystem.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI配置实体类
 */
@Data
public class AIConfig {
    private Long id;
    private String provider;  // dashscope/openai/baidu/tencent
    private String apiKey;
    private String baseUrl;
    private String model;
    private BigDecimal temperature;
    private Integer timeout;
    private String analysisStrategy;  // description/transcribe
    private Boolean isActive;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

