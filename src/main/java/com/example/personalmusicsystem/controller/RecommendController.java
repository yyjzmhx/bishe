package com.example.personalmusicsystem.controller;

import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.RecommendRecord;
import com.example.personalmusicsystem.entity.UploadRecord;
import com.example.personalmusicsystem.service.RecommendService;
import com.example.personalmusicsystem.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推荐控制器
 */
@RestController
@RequestMapping("/api/user")
@Slf4j
public class RecommendController {
    
    @Autowired
    private RecommendService recommendService;
    
    @Autowired
    private UploadService uploadService;
    
    /**
     * 分析音频并生成推荐
     */
    @PostMapping("/analyze/{uploadId}")
    public Result<String> analyzeAudio(@PathVariable Long uploadId,
                                      @RequestAttribute Long userId) {
        try {
            UploadRecord record = uploadService.getById(uploadId);
            if (record == null || !record.getUserId().equals(userId)) {
                return Result.error("上传记录不存在");
            }
            
            recommendService.analyzeAndRecommend(uploadId);
            return Result.success("分析完成，推荐已生成", null);
        } catch (Exception e) {
            log.error("音频分析失败", e);
            return Result.error("分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取推荐列表
     */
    @GetMapping("/recommend/{uploadId}")
    public Result<Map<String, Object>> getRecommendations(@PathVariable Long uploadId,
                                                         @RequestAttribute Long userId) {
        UploadRecord record = uploadService.getById(uploadId);
        if (record == null || !record.getUserId().equals(userId)) {
            return Result.error("上传记录不存在");
        }
        
        List<RecommendRecord> recommendations = recommendService.getRecommendations(uploadId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("uploadId", uploadId);
        result.put("recommendations", recommendations);
        result.put("count", recommendations.size());
        
        return Result.success(result);
    }
}

