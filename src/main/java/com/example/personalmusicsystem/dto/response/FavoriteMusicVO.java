package com.example.personalmusicsystem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏音乐视图对象
 */
@Data
public class FavoriteMusicVO {
    private Long favoriteId;
    private Long id;
    private String title;
    private String artist;
    private String album;
    private String fileUrl;
    private String previewUrl;
    private Integer duration;
    private String genre;
    private String lyrics;
    private String coverUrl;
    private Integer playCount;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime favoriteTime;
}
