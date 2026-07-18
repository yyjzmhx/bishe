package com.example.personalmusicsystem.controller;

import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.UploadRecord;
import com.example.personalmusicsystem.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 上传控制器
 */
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UploadController {
    
    @Autowired
    private UploadService uploadService;
    
    /**
     * 用户上传音频文件
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute Long userId) {
        
        try {
            UploadRecord record = uploadService.saveUploadRecord(userId, file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("uploadId", record.getId());
            result.put("fileUrl", record.getFileUrl());
            result.put("fileName", record.getFileName());
            result.put("fileSize", record.getFileSize());
            result.put("status", record.getStatus());
            
            return Result.success("上传成功", result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取上传记录详情
     */
    @GetMapping("/upload/{id}")
    public Result<UploadRecord> getUploadRecord(@PathVariable Long id,
                                                @RequestAttribute Long userId) {
        UploadRecord record = uploadService.getById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            return Result.error("上传记录不存在");
        }
        return Result.success(record);
    }
    
    /**
     * 删除上传记录
     */
    @DeleteMapping("/upload/{id}")
    public Result<String> deleteUploadRecord(@PathVariable Long id,
                                            @RequestAttribute Long userId) {
        boolean success = uploadService.deleteUploadRecord(id, userId);
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败或记录不存在");
    }
    
    /**
     * 获取用户上传历史
     */
    @GetMapping("/upload/history")
    public Result<Map<String, Object>> getUploadHistory(
            @RequestAttribute Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        List<UploadRecord> records;
        long total;
        
        // 如果有筛选条件，使用带筛选的方法
        if (keyword != null && !keyword.isEmpty() || 
            startDate != null && !startDate.isEmpty() || 
            endDate != null && !endDate.isEmpty()) {
            records = uploadService.getUserUploadRecords(userId, pageNum, pageSize, keyword, startDate, endDate);
            total = uploadService.countUserUploadRecords(userId, keyword, startDate, endDate);
        } else {
            // 没有筛选条件，使用原来的方法
            records = uploadService.getUserUploadRecords(userId, pageNum, pageSize);
            total = uploadService.countUserUploadRecords(userId);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }
}

