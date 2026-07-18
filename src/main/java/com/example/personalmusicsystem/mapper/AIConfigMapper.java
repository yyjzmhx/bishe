package com.example.personalmusicsystem.mapper;

import com.example.personalmusicsystem.entity.AIConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI配置Mapper接口
 */
@Mapper
public interface AIConfigMapper {
    
    /**
     * 根据ID查询配置
     */
    AIConfig selectById(@Param("id") Long id);
    
    /**
     * 查询当前激活的配置
     */
    AIConfig selectActive();
    
    /**
     * 查询所有配置（历史记录）
     */
    List<AIConfig> selectAll();
    
    /**
     * 插入配置
     */
    int insert(AIConfig config);
    
    /**
     * 更新配置
     */
    int update(AIConfig config);
    
    /**
     * 删除配置
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 取消其他配置的激活状态
     */
    int deactivateOthers(@Param("currentId") Long currentId);
}

