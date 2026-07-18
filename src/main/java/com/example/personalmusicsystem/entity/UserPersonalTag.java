package com.example.personalmusicsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户个性化标签实体类
 */
@Data
public class UserPersonalTag {
    private Long id;
    private Long userId;
    private String name;        // 标签名称
    private String color;        // 标签颜色
    private Integer sortOrder;   // 排序顺序
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

