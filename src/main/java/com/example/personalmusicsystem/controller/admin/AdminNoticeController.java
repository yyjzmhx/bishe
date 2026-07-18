package com.example.personalmusicsystem.controller.admin;

import com.example.personalmusicsystem.annotation.RequireAdmin;
import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.Notice;
import com.example.personalmusicsystem.mapper.NoticeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员通知公告控制器
 */
@RestController
@RequestMapping("/api/admin/notices")
@RequireAdmin
@Slf4j
public class AdminNoticeController {
    
    @Autowired
    private NoticeMapper noticeMapper;
    
    /**
     * 获取通知列表（支持搜索、筛选、分页）
     */
    @GetMapping
    public Result<Map<String, Object>> getNoticeList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        int offset = (pageNum - 1) * pageSize;
        List<Notice> list = noticeMapper.selectList(keyword, type, status, offset, pageSize);
        long total = noticeMapper.count(keyword, type, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }
    
    /**
     * 获取通知详情
     */
    @GetMapping("/{id}")
    public Result<Notice> getNoticeDetail(@PathVariable Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            return Result.error("通知不存在");
        }
        return Result.success(notice);
    }
    
    /**
     * 添加通知
     */
    @PostMapping
    public Result<Map<String, Object>> addNotice(@RequestBody Notice notice) {
        try {
            if (notice.getStatus() == null) {
                notice.setStatus(0); // 默认草稿
            }
            if (notice.getType() == null || notice.getType().isEmpty()) {
                notice.setType("SYSTEM");
            }
            // 如果状态为发布，设置发布时间
            if (notice.getStatus() == 1 && notice.getPublishTime() == null) {
                notice.setPublishTime(LocalDateTime.now());
            }
            
            boolean success = noticeMapper.insert(notice) > 0;
            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("noticeId", notice.getId());
                return Result.success("添加成功", result);
            }
            return Result.error("添加失败");
        } catch (Exception e) {
            log.error("添加通知失败", e);
            return Result.error("添加失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新通知
     */
    @PutMapping("/{id}")
    public Result<String> updateNotice(@PathVariable Long id, @RequestBody Notice notice) {
        try {
            notice.setId(id);
            // 如果状态从草稿变为发布，设置发布时间
            Notice oldNotice = noticeMapper.selectById(id);
            if (oldNotice != null && oldNotice.getStatus() == 0 && notice.getStatus() == 1) {
                if (notice.getPublishTime() == null) {
                    notice.setPublishTime(LocalDateTime.now());
                }
            }
            
            boolean success = noticeMapper.update(notice) > 0;
            if (success) {
                return Result.success("更新成功", null);
            }
            return Result.error("更新失败");
        } catch (Exception e) {
            log.error("更新通知失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteNotice(@PathVariable Long id) {
        boolean success = noticeMapper.deleteById(id) > 0;
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }
    
    /**
     * 发布/取消发布通知
     */
    @PutMapping("/{id}/publish")
    public Result<String> publishNotice(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Boolean publish = (Boolean) request.get("publish");
            Notice notice = new Notice();
            notice.setId(id);
            notice.setStatus(publish != null && publish ? 1 : 0);
            if (publish != null && publish) {
                notice.setPublishTime(LocalDateTime.now());
            }
            
            boolean success = noticeMapper.update(notice) > 0;
            if (success) {
                return Result.success(publish ? "发布成功" : "已取消发布", null);
            }
            return Result.error("操作失败");
        } catch (Exception e) {
            log.error("发布通知失败", e);
            return Result.error("操作失败: " + e.getMessage());
        }
    }
}

