package com.example.personalmusicsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户上传记录实体类
 */
@Data
public class UploadRecord {
    private Long id;
    private Long userId;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String status;  // UPLOADING/ANALYZING/COMPLETED/FAILED
    private String features;  // JSON字符串，提取的音频特征
    private String analysisResult;  // 分析结果描述
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

