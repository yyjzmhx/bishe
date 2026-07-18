package com.example.personalmusicsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 反馈实体类
 */
@Data
public class Feedback {
    private Long id;
    private Long userId;
    private Long recommendId;
    private Long musicId;
    private Long parentId;  // 父评论ID（回复时使用）
    private String type;  // LIKE/DISLIKE/COMMENT
    private String content;
    private LocalDateTime createTime;
}

