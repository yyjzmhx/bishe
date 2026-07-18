package com.example.personalmusicsystem.controller.admin;

import com.example.personalmusicsystem.annotation.RequireAdmin;
import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.SiteContent;
import com.example.personalmusicsystem.mapper.SiteContentMapper;
import com.example.personalmusicsystem.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员站点内容控制器
 */
@RestController
@RequestMapping("/api/admin/site-content")
@RequireAdmin
@Slf4j
public class AdminSiteContentController {
    
    @Autowired
    private SiteContentMapper siteContentMapper;
    
    @Autowired
    private FileService fileService;
    
    /**
     * 获取站点内容列表（支持筛选、分页）
     */
    @GetMapping
    public Result<Map<String, Object>> getContentList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        int offset = (pageNum - 1) * pageSize;
        List<SiteContent> list = siteContentMapper.selectList(type, status, offset, pageSize);
        long total = siteContentMapper.count(type, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }
    
    /**
     * 获取站点内容详情
     */
    @GetMapping("/{id}")
    public Result<SiteContent> getContentDetail(@PathVariable Long id) {
        SiteContent content = siteContentMapper.selectById(id);
        if (content == null) {
            return Result.error("内容不存在");
        }
        return Result.success(content);
    }
    
    /**
     * 添加站点内容
     */
    @PostMapping
    public Result<Map<String, Object>> addContent(
            @RequestParam String type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String linkUrl,
            @RequestParam(required = false) String content,
            @RequestParam(defaultValue = "0") Integer sortOrder) {
        try {
            SiteContent siteContent = new SiteContent();
            siteContent.setType(type);
            siteContent.setTitle(title);
            siteContent.setContent(content);
            siteContent.setLinkUrl(linkUrl);
            siteContent.setSortOrder(sortOrder);
            siteContent.setStatus(1); // 默认启用
            
            // 上传图片
            if (imageFile != null && !imageFile.isEmpty()) {
                Map<String, String> imageInfo = fileService.saveCoverImage(imageFile, null);
                siteContent.setImageUrl(imageInfo.get("fileUrl"));
            } else if (imageUrl != null && !imageUrl.isEmpty()) {
                siteContent.setImageUrl(imageUrl);
            }
            
            int insertResult = siteContentMapper.insert(siteContent);
            boolean success = insertResult > 0;
            if (success) {
                Map<String, Object> data = new HashMap<>();
                data.put("contentId", siteContent.getId());
                return Result.success("添加成功", data);
            }
            return Result.error("添加失败");
        } catch (Exception e) {
            log.error("添加站点内容失败", e);
            return Result.error("添加失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新站点内容
     */
    @PutMapping("/{id}")
    public Result<String> updateContent(@PathVariable Long id, @RequestBody SiteContent siteContent) {
        try {
            siteContent.setId(id);
            boolean success = siteContentMapper.update(siteContent) > 0;
            if (success) {
                return Result.success("更新成功", null);
            }
            return Result.error("更新失败");
        } catch (Exception e) {
            log.error("更新站点内容失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传图片
     */
    @PostMapping("/{id}/image")
    public Result<Map<String, String>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            SiteContent content = siteContentMapper.selectById(id);
            if (content == null) {
                return Result.error("内容不存在");
            }
            
            String oldImageUrl = content.getImageUrl();
            
            Map<String, String> fileInfo = fileService.saveCoverImage(file, null);
            String newImageUrl = fileInfo.get("fileUrl");
            
            SiteContent updateContent = new SiteContent();
            updateContent.setId(id);
            updateContent.setImageUrl(newImageUrl);
            
            int updateResult = siteContentMapper.update(updateContent);
            boolean updateSuccess = updateResult > 0;
            
            if (!updateSuccess) {
                try {
                    fileService.deleteFile(newImageUrl);
                } catch (Exception deleteEx) {
                    log.warn("回滚删除新上传图片失败: {}", newImageUrl, deleteEx);
                }
                return Result.error("数据库更新图片URL失败");
            }
            
            if (oldImageUrl != null && !oldImageUrl.isEmpty() && !oldImageUrl.equals(newImageUrl)) {
                try {
                    fileService.deleteFile(oldImageUrl);
                    log.info("删除旧图片成功: {}", oldImageUrl);
                } catch (Exception e) {
                    log.warn("删除旧图片失败: {}", oldImageUrl, e);
                }
            }
            
            return Result.success("图片上传成功", fileInfo);
        } catch (Exception e) {
            log.error("上传图片失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除站点内容
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteContent(@PathVariable Long id) {
        try {
            SiteContent content = siteContentMapper.selectById(id);
            if (content != null && content.getImageUrl() != null && !content.getImageUrl().isEmpty()) {
                try {
                    fileService.deleteFile(content.getImageUrl());
                } catch (Exception e) {
                    log.warn("删除图片失败: {}", e.getMessage());
                }
            }
            
            boolean success = siteContentMapper.deleteById(id) > 0;
            if (success) {
                return Result.success("删除成功", null);
            }
            return Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除站点内容失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 启用/禁用站点内容
     */
    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            Integer status = request.get("status");
            SiteContent content = new SiteContent();
            content.setId(id);
            content.setStatus(status);
            
            boolean success = siteContentMapper.update(content) > 0;
            if (success) {
                return Result.success("状态更新成功", null);
            }
            return Result.error("状态更新失败");
        } catch (Exception e) {
            log.error("更新状态失败", e);
            return Result.error("状态更新失败: " + e.getMessage());
        }
    }
}

