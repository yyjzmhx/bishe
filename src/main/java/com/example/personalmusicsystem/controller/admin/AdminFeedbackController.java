package com.example.personalmusicsystem.controller.admin;

import com.example.personalmusicsystem.annotation.RequireAdmin;
import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.dto.response.FeedbackVO;
import com.example.personalmusicsystem.entity.Feedback;
import com.example.personalmusicsystem.mapper.FeedbackMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员反馈管理控制器
 */
@RestController
@RequestMapping("/api/admin/feedback")
@RequireAdmin
@Slf4j
public class AdminFeedbackController {
    
    @Autowired
    private FeedbackMapper feedbackMapper;
    
    /**
     * 获取反馈列表（支持搜索、筛选、分页）
     */
    @GetMapping
    public Result<Map<String, Object>> getFeedbackList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        int offset = (pageNum - 1) * pageSize;
        List<FeedbackVO> list = feedbackMapper.selectAdminList(keyword, type, startDate, endDate, offset, pageSize);
        long total = feedbackMapper.countAdminList(keyword, type, startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }
    
    /**
     * 获取反馈详情
     */
    @GetMapping("/{id}")
    public Result<Feedback> getFeedbackDetail(@PathVariable Long id) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            return Result.error("反馈不存在");
        }
        return Result.success(feedback);
    }
    
    /**
     * 删除反馈
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteFeedback(@PathVariable Long id) {
        boolean success = feedbackMapper.deleteById(id) > 0;
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }
}

