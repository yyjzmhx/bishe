package com.example.personalmusicsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 音乐实体类
 */
@Data
public class Music {
    private Long id;
    private String title;
    private String artist;
    private String album;
    private String fileUrl;
    private String previewUrl;
    private Integer duration;
    private String genre;
    private String lyrics;  // 歌词内容(LRC格式)
    private String features;  // JSON字符串，MFCC等特征向量
    private String coverUrl;
    private Integer playCount;
    private Integer likeCount;
    private Integer status;  // 1-正常 0-下架
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

