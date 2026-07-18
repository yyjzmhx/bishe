package com.example.personalmusicsystem.controller;

import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.UserPersonalTag;
import com.example.personalmusicsystem.service.UserPersonalTagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/personal-tags")
@Slf4j
public class UserPersonalTagController {
    
    @Autowired
    private UserPersonalTagService tagService;
    
    /**
     * 创建标签
     */
    @PostMapping
    public Result<UserPersonalTag> createTag(@RequestBody Map<String, String> request,
                                            @RequestAttribute Long userId) {
        try {
            String name = request.get("name");
            String color = request.getOrDefault("color", "#409EFF");
            
            UserPersonalTag tag = tagService.createTag(userId, name, color);
            return Result.success(tag);
        } catch (Exception e) {
            log.error("创建标签失败，用户ID: {}", userId, e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 更新标签
     */
    @PutMapping("/{tagId}")
    public Result<UserPersonalTag> updateTag(@PathVariable Long tagId,
                                            @RequestBody Map<String, String> request,
                                            @RequestAttribute Long userId) {
        try {
            String name = request.get("name");
            String color = request.get("color");
            
            UserPersonalTag tag = tagService.updateTag(userId, tagId, name, color);
            return Result.success(tag);
        } catch (Exception e) {
            log.error("更新标签失败，用户ID: {}, 标签ID: {}", userId, tagId, e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除标签
     */
    @DeleteMapping("/{tagId}")
    public Result<String> deleteTag(@PathVariable Long tagId,
                                    @RequestAttribute Long userId) {
        try {
            boolean success = tagService.deleteTag(userId, tagId);
            return success ? Result.success("删除成功", null) : Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除标签失败，用户ID: {}, 标签ID: {}", userId, tagId, e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户的所有标签
     */
    @GetMapping
    public Result<List<UserPersonalTag>> getUserTags(@RequestAttribute Long userId) {
        List<UserPersonalTag> tags = tagService.getUserTags(userId);
        return Result.success(tags);
    }
    
    /**
     * 批量更新标签排序
     */
    @PutMapping("/order")
    public Result<String> updateTagOrder(@RequestBody Map<String, List<Long>> request,
                                        @RequestAttribute Long userId) {
        try {
            List<Long> tagIds = request.get("tagIds");
            boolean success = tagService.updateTagOrder(userId, tagIds);
            return success ? Result.success("排序更新成功", null) : Result.error("排序更新失败");
        } catch (Exception e) {
            log.error("更新标签排序失败，用户ID: {}", userId, e);
            return Result.error(e.getMessage());
        }
    }
}

