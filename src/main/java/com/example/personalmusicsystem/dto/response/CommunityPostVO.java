package com.example.personalmusicsystem.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 社区动态视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommunityPostVO extends CommentVO {
    private String musicTitle;
    private String musicArtist;
    private String musicCoverUrl;
}
