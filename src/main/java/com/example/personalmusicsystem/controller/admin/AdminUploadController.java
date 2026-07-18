package com.example.personalmusicsystem.controller.admin;

import com.example.personalmusicsystem.annotation.RequireAdmin;
import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.dto.response.UploadRecordVO;
import com.example.personalmusicsystem.entity.UploadRecord;
import com.example.personalmusicsystem.mapper.UploadRecordMapper;
import com.example.personalmusicsystem.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员上传管理控制器
 */
@RestController
@RequestMapping("/api/admin/uploads")
@RequireAdmin
@Slf4j
public class AdminUploadController {
    
    @Autowired
    private UploadRecordMapper uploadRecordMapper;
    
    @Autowired
    private FileService fileService;
    
    /**
     * 获取上传列表（支持搜索、筛选、分页）
     */
    @GetMapping
    public Result<Map<String, Object>> getUploadList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        int offset = (pageNum - 1) * pageSize;
        List<UploadRecordVO> list = uploadRecordMapper.selectList(keyword, status, startDate, endDate, offset, pageSize);
        long total = uploadRecordMapper.countList(keyword, status, startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }
    
    /**
     * 获取上传详情
     */
    @GetMapping("/{id}")
    public Result<UploadRecord> getUploadDetail(@PathVariable Long id) {
        UploadRecord record = uploadRecordMapper.selectById(id);
        if (record == null) {
            return Result.error("上传记录不存在");
        }
        return Result.success(record);
    }
    
    /**
     * 删除上传记录
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteUpload(@PathVariable Long id) {
        try {
            UploadRecord record = uploadRecordMapper.selectById(id);
            if (record == null) {
                return Result.error("上传记录不存在");
            }
            
            // 删除MinIO中的文件
            if (record.getFileUrl() != null && !record.getFileUrl().isEmpty()) {
                try {
                    fileService.deleteFile(record.getFileUrl());
                    log.info("删除上传文件成功: {}", record.getFileUrl());
                } catch (Exception e) {
                    log.warn("删除上传文件失败: {}", e.getMessage());
                }
            }
            
            // 删除数据库记录
            boolean success = uploadRecordMapper.deleteById(id) > 0;
            if (success) {
                return Result.success("删除成功", null);
            }
            return Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除上传记录失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}

