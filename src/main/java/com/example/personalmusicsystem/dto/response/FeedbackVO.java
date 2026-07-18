package com.example.personalmusicsystem.dto.response;

import com.example.personalmusicsystem.entity.Feedback;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 反馈视图对象（包含用户和音乐信息）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FeedbackVO extends Feedback {
    private String username;
    private String nickname;
    private String musicTitle;
    private String musicArtist;
}

