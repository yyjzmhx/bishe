package com.example.personalmusicsystem.mapper;

import com.example.personalmusicsystem.entity.RecommendRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 推荐记录Mapper接口
 */
@Mapper
public interface RecommendRecordMapper {
    
    /**
     * 插入推荐记录
     */
    int insert(RecommendRecord recommendRecord);
    
    /**
     * 批量插入推荐记录
     */
    int insertBatch(@Param("list") List<RecommendRecord> list);
    
    /**
     * 根据上传ID查询推荐记录
     */
    List<RecommendRecord> selectByUploadId(@Param("uploadId") Long uploadId);
    
    /**
     * 删除推荐记录
     */
    int deleteByUploadId(@Param("uploadId") Long uploadId);
    
    /**
     * 统计所有推荐记录数量
     */
    long countAll();

    /**
     * 统计推荐平均相似度
     */
    Double selectAverageSimilarity();
}

