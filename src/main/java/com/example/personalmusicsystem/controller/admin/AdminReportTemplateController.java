package com.example.personalmusicsystem.controller.admin;

import com.alibaba.fastjson2.JSON;
import com.example.personalmusicsystem.annotation.RequireAdmin;
import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.ReportTemplate;
import com.example.personalmusicsystem.mapper.ReportTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 报告模板管理控制器
 */
@RestController
@RequestMapping("/api/admin/ai/template")
@RequireAdmin
@Slf4j
public class AdminReportTemplateController {
    
    @Autowired
    private ReportTemplateMapper reportTemplateMapper;
    
    /**
     * 获取模板列表
     */
    @GetMapping("/list")
    public Result<List<ReportTemplate>> getTemplateList() {
        List<ReportTemplate> templates = reportTemplateMapper.selectAll();
        return Result.success(templates);
    }
    
    /**
     * 根据ID获取模板
     */
    @GetMapping("/{id}")
    public Result<ReportTemplate> getTemplateById(@PathVariable Long id) {
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template == null) {
            return Result.error("模板不存在");
        }
        return Result.success(template);
    }
    
    /**
     * 获取默认模板
     */
    @GetMapping("/default")
    public Result<ReportTemplate> getDefaultTemplate() {
        ReportTemplate template = reportTemplateMapper.selectDefault();
        if (template == null) {
            return Result.error("默认模板不存在");
        }
        return Result.success(template);
    }
    
    /**
     * 创建模板
     */
    @PostMapping("/create")
    public Result<Void> createTemplate(@RequestBody Map<String, Object> data) {
        try {
            ReportTemplate template = new ReportTemplate();
            template.setName((String) data.get("name"));
            template.setDescription((String) data.get("description"));
            
            // 处理config字段
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) data.get("config");
            if (config != null) {
                template.setConfig(JSON.toJSONString(config));
            }
            
            template.setIsDefault(false);
            template.setStatus((Integer) data.getOrDefault("status", 1));
            
            reportTemplateMapper.insert(template);
            return Result.success(null);
        } catch (Exception e) {
            log.error("创建模板失败", e);
            return Result.error("创建模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            ReportTemplate template = reportTemplateMapper.selectById(id);
            if (template == null) {
                return Result.error("模板不存在");
            }
            
            template.setName((String) data.get("name"));
            template.setDescription((String) data.get("description"));
            
            // 处理config字段
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) data.get("config");
            if (config != null) {
                template.setConfig(JSON.toJSONString(config));
            }
            
            template.setStatus((Integer) data.getOrDefault("status", 1));
            
            reportTemplateMapper.update(template);
            return Result.success(null);
        } catch (Exception e) {
            log.error("更新模板失败", e);
            return Result.error("更新模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 设为默认模板
     */
    @PostMapping("/{id}/set-default")
    public Result<Void> setDefaultTemplate(@PathVariable Long id) {
        try {
            ReportTemplate template = reportTemplateMapper.selectById(id);
            if (template == null) {
                return Result.error("模板不存在");
            }
            
            // 取消其他模板的默认状态
            reportTemplateMapper.unsetDefaultOthers(id);
            
            // 设置当前模板为默认
            template.setIsDefault(true);
            reportTemplateMapper.update(template);
            
            return Result.success(null);
        } catch (Exception e) {
            log.error("设置默认模板失败", e);
            return Result.error("设置默认模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        try {
            ReportTemplate template = reportTemplateMapper.selectById(id);
            if (template == null) {
                return Result.error("模板不存在");
            }
            
            if (template.getIsDefault() != null && template.getIsDefault()) {
                return Result.error("不能删除默认模板，请先设置其他模板为默认");
            }
            
            reportTemplateMapper.deleteById(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除模板失败", e);
            return Result.error("删除模板失败: " + e.getMessage());
        }
    }
}

