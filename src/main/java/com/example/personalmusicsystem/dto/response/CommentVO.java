package com.example.personalmusicsystem.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论VO
 */
@Data
public class CommentVO {
    private Long id;
    private Long userId;
    private Long musicId;
    private Long parentId;  // 父评论ID
    private String content;
    private LocalDateTime createTime;
    private String username;
    private String nickname;
    private String avatar;
    private Long likeCount;  // 点赞数
    private Boolean isLiked; // 当前用户是否已点赞
    private Long replyCount; // 回复数
    private List<CommentVO> replies; // 回复列表
}

