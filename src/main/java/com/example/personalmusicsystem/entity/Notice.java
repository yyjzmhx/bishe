package com.example.personalmusicsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知公告实体类
 */
@Data
public class Notice {
    private Long id;
    private String title;
    private String content;
    private String type;  // SYSTEM/UPDATE/NEW_MUSIC
    private Integer status;  // 1-发布 0-草稿
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
