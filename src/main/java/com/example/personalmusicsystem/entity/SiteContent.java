package com.example.personalmusicsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 站点内容实体类
 */
@Data
public class SiteContent {
    private Long id;
    private String type;  // CAROUSEL/HOT_PLAYLIST/BANNER
    private String title;
    private String content;  // JSON格式
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;
    private Integer status;  // 1-启用 0-禁用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
