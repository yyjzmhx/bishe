package com.example.personalmusicsystem.mapper;

import com.example.personalmusicsystem.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知公告Mapper接口
 */
@Mapper
public interface NoticeMapper {
    
    /**
     * 根据ID查询通知
     */
    Notice selectById(@Param("id") Long id);
    
    /**
     * 插入通知
     */
    int insert(Notice notice);
    
    /**
     * 更新通知
     */
    int update(Notice notice);
    
    /**
     * 删除通知
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 查询通知列表（支持搜索、筛选、分页）
     */
    List<Notice> selectList(@Param("keyword") String keyword,
                            @Param("type") String type,
                            @Param("status") Integer status,
                            @Param("offset") Integer offset,
                            @Param("limit") Integer limit);
    
    /**
     * 统计通知数量
     */
    long count(@Param("keyword") String keyword,
              @Param("type") String type,
              @Param("status") Integer status);
}

