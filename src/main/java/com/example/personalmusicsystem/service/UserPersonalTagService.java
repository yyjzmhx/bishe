package com.example.personalmusicsystem.service;

import com.example.personalmusicsystem.entity.UserPersonalTag;
import com.example.personalmusicsystem.mapper.UserPersonalTagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class UserPersonalTagService {
    
    @Autowired
    private UserPersonalTagMapper tagMapper;
    
    private static final int MAX_TAGS = 10; // 最多10个标签
    
    /**
     * 创建标签
     */
    @Transactional
    public UserPersonalTag createTag(Long userId, String name, String color) {
        // 检查标签数量限制
        int count = tagMapper.countByUserId(userId);
        if (count >= MAX_TAGS) {
            throw new RuntimeException("最多只能添加" + MAX_TAGS + "个标签");
        }
        
        // 检查标签名是否为空
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("标签名称不能为空");
        }
        
        // 检查标签名长度
        if (name.trim().length() > 20) {
            throw new RuntimeException("标签名称不能超过20个字符");
        }
        
        UserPersonalTag tag = new UserPersonalTag();
        tag.setUserId(userId);
        tag.setName(name.trim());
        tag.setColor(color != null ? color : "#409EFF");
        tag.setSortOrder(count); // 设置排序顺序
        
        tagMapper.insert(tag);
        log.info("创建用户标签成功，用户ID: {}, 标签名称: {}", userId, name);
        return tag;
    }
    
    /**
     * 更新标签
     */
    @Transactional
    public UserPersonalTag updateTag(Long userId, Long tagId, String name, String color) {
        UserPersonalTag tag = tagMapper.selectById(tagId, userId);
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }
        
        if (name != null && !name.trim().isEmpty()) {
            if (name.trim().length() > 20) {
                throw new RuntimeException("标签名称不能超过20个字符");
            }
            tag.setName(name.trim());
        }
        
        if (color != null) {
            tag.setColor(color);
        }
        
        tagMapper.update(tag);
        log.info("更新用户标签成功，用户ID: {}, 标签ID: {}", userId, tagId);
        return tag;
    }
    
    /**
     * 删除标签
     */
    @Transactional
    public boolean deleteTag(Long userId, Long tagId) {
        int result = tagMapper.deleteById(tagId, userId);
        if (result > 0) {
            log.info("删除用户标签成功，用户ID: {}, 标签ID: {}", userId, tagId);
            return true;
        }
        return false;
    }
    
    /**
     * 获取用户的所有标签
     */
    public List<UserPersonalTag> getUserTags(Long userId) {
        return tagMapper.selectByUserId(userId);
    }
    
    /**
     * 批量更新标签排序
     */
    @Transactional
    public boolean updateTagOrder(Long userId, List<Long> tagIds) {
        for (int i = 0; i < tagIds.size(); i++) {
            UserPersonalTag tag = tagMapper.selectById(tagIds.get(i), userId);
            if (tag != null) {
                tag.setSortOrder(i);
                tagMapper.update(tag);
            }
        }
        return true;
    }
}

