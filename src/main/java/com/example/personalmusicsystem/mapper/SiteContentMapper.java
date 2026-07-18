package com.example.personalmusicsystem.mapper;

import com.example.personalmusicsystem.entity.SiteContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 站点内容Mapper接口
 */
@Mapper
public interface SiteContentMapper {
    
    /**
     * 根据ID查询站点内容
     */
    SiteContent selectById(@Param("id") Long id);
    
    /**
     * 插入站点内容
     */
    int insert(SiteContent siteContent);
    
    /**
     * 更新站点内容
     */
    int update(SiteContent siteContent);
    
    /**
     * 删除站点内容
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 查询站点内容列表（支持搜索、筛选、分页）
     */
    List<SiteContent> selectList(@Param("type") String type,
                                @Param("status") Integer status,
                                @Param("offset") Integer offset,
                                @Param("limit") Integer limit);
    
    /**
     * 统计站点内容数量
     */
    long count(@Param("type") String type,
              @Param("status") Integer status);
}

