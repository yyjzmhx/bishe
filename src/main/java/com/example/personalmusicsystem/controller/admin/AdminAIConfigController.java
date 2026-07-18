package com.example.personalmusicsystem.controller.admin;

import com.example.personalmusicsystem.annotation.RequireAdmin;
import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.AIConfig;
import com.example.personalmusicsystem.mapper.AIConfigMapper;
import com.example.personalmusicsystem.service.ai.DynamicAIChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI配置管理控制器
 */
@RestController
@RequestMapping("/api/admin/ai/config")
@RequireAdmin
@Slf4j
public class AdminAIConfigController {
    
    @Autowired
    private AIConfigMapper aiConfigMapper;

    @Autowired
    private DynamicAIChatService dynamicAIChatService;
    
    /**
     * 获取当前激活的配置
     */
    @GetMapping("/current")
    public Result<AIConfig> getCurrentConfig() {
        AIConfig config = aiConfigMapper.selectActive();
        if (config == null) {
            // 如果没有激活的配置，返回默认配置
            config = new AIConfig();
            config.setProvider("dashscope");
            config.setModel("qwen-turbo");
            config.setTemperature(java.math.BigDecimal.valueOf(0.7));
            config.setTimeout(30000);
            config.setAnalysisStrategy("description");
            config.setIsActive(true);
        }
        return Result.success(config);
    }
    
    /**
     * 获取配置历史
     */
    @GetMapping("/history")
    public Result<List<AIConfig>> getConfigHistory() {
        List<AIConfig> configs = aiConfigMapper.selectAll();
        return Result.success(configs);
    }
    
    /**
     * 根据ID获取配置
     */
    @GetMapping("/{id}")
    public Result<AIConfig> getConfigById(@PathVariable Long id) {
        AIConfig config = aiConfigMapper.selectById(id);
        if (config == null) {
            return Result.error("配置不存在");
        }
        return Result.success(config);
    }
    
    /**
     * 测试连接
     */
    @PostMapping("/test")
    public Result<Map<String, Object>> testConnection(@RequestBody AIConfig config) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 这里可以调用实际的AI服务进行测试
            // 简化实现：只验证必填字段
            if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
                result.put("success", false);
                result.put("message", "API Key不能为空");
                return Result.success(result);
            }
            
            if (config.getModel() == null || config.getModel().isEmpty()) {
                result.put("success", false);
                result.put("message", "模型名称不能为空");
                return Result.success(result);
            }
            
            // TODO: 实际调用AI服务测试连接
            // 这里暂时返回成功
            dynamicAIChatService.testConnection(config);
            result.put("success", true);
            result.put("message", "连接测试成功");
            
        } catch (Exception e) {
            log.error("测试连接失败", e);
            result.put("success", false);
            result.put("message", "连接测试失败: " + e.getMessage());
        }
        
        return Result.success(result);
    }
    
    /**
     * 保存配置
     */
    @PostMapping("/save")
    public Result<Void> saveConfig(@RequestBody AIConfig config) {
        try {
            if (config.getId() != null) {
                // 更新配置
                if (config.getIsActive() != null && config.getIsActive()) {
                    // 如果激活此配置，取消其他配置的激活状态
                    aiConfigMapper.deactivateOthers(config.getId());
                }
                aiConfigMapper.update(config);
            } else {
                // 新增配置
                if (config.getIsActive() == null) {
                    config.setIsActive(false);
                }
                if (config.getIsActive()) {
                    // 如果激活此配置，取消其他配置的激活状态
                    aiConfigMapper.insert(config);
                    aiConfigMapper.deactivateOthers(config.getId());
                } else {
                    aiConfigMapper.insert(config);
                }
            }
            
            return Result.success(null);
        } catch (Exception e) {
            log.error("保存配置失败", e);
            return Result.error("保存配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        try {
            AIConfig config = aiConfigMapper.selectById(id);
            if (config == null) {
                return Result.error("配置不存在");
            }
            
            if (config.getIsActive() != null && config.getIsActive()) {
                return Result.error("不能删除激活中的配置，请先激活其他配置");
            }
            
            aiConfigMapper.deleteById(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除配置失败", e);
            return Result.error("删除配置失败: " + e.getMessage());
        }
    }
}

