package com.example.personalmusicsystem.mapper;

import com.example.personalmusicsystem.entity.ReportTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报告模板Mapper接口
 */
@Mapper
public interface ReportTemplateMapper {
    
    /**
     * 根据ID查询模板
     */
    ReportTemplate selectById(@Param("id") Long id);
    
    /**
     * 查询默认模板
     */
    ReportTemplate selectDefault();
    
    /**
     * 查询所有模板
     */
    List<ReportTemplate> selectAll();
    
    /**
     * 插入模板
     */
    int insert(ReportTemplate template);
    
    /**
     * 更新模板
     */
    int update(ReportTemplate template);
    
    /**
     * 删除模板
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 取消其他模板的默认状态
     */
    int unsetDefaultOthers(@Param("currentId") Long currentId);
}

