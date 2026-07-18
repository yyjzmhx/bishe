package com.example.personalmusicsystem.mapper;

import com.example.personalmusicsystem.entity.Music;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 音乐Mapper接口
 */
@Mapper
public interface MusicMapper {
    
    /**
     * 根据ID查询音乐
     */
    Music selectById(@Param("id") Long id);
    
    /**
     * 插入音乐
     */
    int insert(Music music);
    
    /**
     * 更新音乐
     */
    int update(Music music);
    
    /**
     * 删除音乐
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 查询音乐列表（分页、搜索）
     */
    List<Music> selectList(@Param("keyword") String keyword,
                          @Param("artist") String artist,
                          @Param("genre") String genre,
                          @Param("status") Integer status,
                          @Param("offset") Integer offset,
                          @Param("limit") Integer limit);
    
    /**
     * 统计音乐数量
     */
    long count(@Param("keyword") String keyword,
               @Param("artist") String artist,
               @Param("genre") String genre,
               @Param("status") Integer status);
    
    /**
     * 根据特征查询相似音乐（用于推荐）
     */
    List<Music> selectByFeatures(@Param("features") String features,
                                @Param("limit") Integer limit);

    /**
     * 统计曲库风格分布
     */
    List<Map<String, Object>> selectGenreDistribution(@Param("limit") Integer limit);
}

