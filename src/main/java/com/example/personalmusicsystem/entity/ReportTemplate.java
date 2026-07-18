package com.example.personalmusicsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 报告模板实体类
 */
@Data
public class ReportTemplate {
    private Long id;
    private String name;
    private String description;
    private String config;  // JSON字符串，包含fields和layout
    private Boolean isDefault;
    private Integer status;  // 1启用 0禁用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

