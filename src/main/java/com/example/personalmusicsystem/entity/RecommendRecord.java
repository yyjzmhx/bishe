package com.example.personalmusicsystem.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推荐记录实体类
 */
@Data
public class RecommendRecord {
    private Long id;
    private Long uploadId;
    private Long musicId;
    private BigDecimal similarity;  // 相似度分数(0-1)
    private Integer rank;  // 推荐排名
    private LocalDateTime createTime;
    
    // 关联查询字段
    private Music music;
}

