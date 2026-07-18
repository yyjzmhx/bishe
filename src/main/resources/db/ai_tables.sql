-- ==================== AI配置相关表 ====================

-- AI配置表
CREATE TABLE IF NOT EXISTS `ai_config` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
  `provider` VARCHAR(50) NOT NULL COMMENT '提供商：dashscope/openai/baidu/tencent',
  `api_key` VARCHAR(500) NOT NULL COMMENT 'API密钥（加密存储）',
  `base_url` VARCHAR(500) COMMENT 'Base URL',
  `model` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `temperature` DECIMAL(3,2) DEFAULT 0.7 COMMENT '温度参数（0-2）',
  `timeout` INT DEFAULT 30000 COMMENT '超时时间(ms)',
  `analysis_strategy` VARCHAR(20) DEFAULT 'description' COMMENT '分析策略：description/transcribe',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否激活：1激活 0禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_provider` (`provider`),
  INDEX `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI服务配置表';

-- 报告模板表
CREATE TABLE IF NOT EXISTS `report_template` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
  `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `description` TEXT COMMENT '模板描述',
  `config` JSON COMMENT '模板配置（字段、布局等）',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认模板：1是 0否',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_status` (`status`),
  INDEX `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析报告模板表';

-- 插入默认AI配置
INSERT INTO `ai_config` (`provider`, `api_key`, `base_url`, `model`, `temperature`, `timeout`, `analysis_strategy`, `is_active`)
VALUES ('dashscope', '', 'https://dashscope.aliyuncs.com/api/v1', 'qwen-turbo', 0.7, 30000, 'description', 1)
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

-- 插入默认报告模板
INSERT INTO `report_template` (`name`, `description`, `config`, `is_default`, `status`)
VALUES (
  '默认模板',
  '包含所有分析字段的标准模板',
  JSON_OBJECT(
    'fields', JSON_ARRAY('recommendationReason', 'style', 'emotion', 'rhythm', 'atmosphere', 'instruments', 'description', 'featureVector', 'visualization'),
    'layout', 'card'
  ),
  1,
  1
)
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

