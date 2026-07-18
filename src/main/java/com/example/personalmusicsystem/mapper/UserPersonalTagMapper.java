package com.example.personalmusicsystem.mapper;

import com.example.personalmusicsystem.entity.UserPersonalTag;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface UserPersonalTagMapper {
    // 插入标签
    int insert(UserPersonalTag tag);
    
    // 更新标签
    int update(UserPersonalTag tag);
    
    // 删除标签
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);
    
    // 查询用户的所有标签
    List<UserPersonalTag> selectByUserId(@Param("userId") Long userId);
    
    // 根据ID查询标签
    UserPersonalTag selectById(@Param("id") Long id, @Param("userId") Long userId);
    
    // 统计用户标签数量
    int countByUserId(@Param("userId") Long userId);
}

