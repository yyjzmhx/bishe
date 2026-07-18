package com.example.personalmusicsystem.mapper;

import com.example.personalmusicsystem.dto.response.UploadRecordVO;
import com.example.personalmusicsystem.entity.UploadRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 上传记录Mapper接口
 */
@Mapper
public interface UploadRecordMapper {
    
    /**
     * 根据ID查询上传记录
     */
    UploadRecord selectById(@Param("id") Long id);
    
    /**
     * 根据用户ID查询上传记录列表
     */
    List<UploadRecord> selectByUserId(@Param("userId") Long userId,
                                      @Param("offset") Integer offset,
                                      @Param("limit") Integer limit);
    
    /**
     * 插入上传记录
     */
    int insert(UploadRecord uploadRecord);
    
    /**
     * 更新上传记录
     */
    int update(UploadRecord uploadRecord);
    
    /**
     * 删除上传记录
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 统计用户上传记录数量（userId为null时统计全部）
     */
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * 统计所有上传记录数量
     */
    long countAll();

    /**
     * 按状态统计上传记录数量
     */
    long countByStatus(@Param("status") String status);
    
    /**
     * 管理员查询上传记录列表（支持搜索、筛选、分页）
     */
    List<UploadRecordVO> selectList(@Param("keyword") String keyword,
                                    @Param("status") String status,
                                    @Param("startDate") String startDate,
                                    @Param("endDate") String endDate,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);
    
    /**
     * 管理员统计上传记录数量
     */
    long countList(@Param("keyword") String keyword,
                  @Param("status") String status,
                  @Param("startDate") String startDate,
                  @Param("endDate") String endDate);
    
    /**
     * 用户查询上传记录列表（支持搜索、筛选、分页）
     */
    List<UploadRecord> selectUserList(@Param("userId") Long userId,
                                     @Param("keyword") String keyword,
                                     @Param("startDate") String startDate,
                                     @Param("endDate") String endDate,
                                     @Param("offset") Integer offset,
                                     @Param("limit") Integer limit);
    
    /**
     * 用户统计上传记录数量
     */
    long countUserList(@Param("userId") Long userId,
                      @Param("keyword") String keyword,
                      @Param("startDate") String startDate,
                      @Param("endDate") String endDate);
}

