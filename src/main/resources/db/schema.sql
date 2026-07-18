-- ==================== 个性化音乐推荐系统数据库表结构 ====================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS music_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE music_system;

-- ==================== 用户相关表 ====================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
  `phone` VARCHAR(20) UNIQUE COMMENT '手机号',
  `email` VARCHAR(100) UNIQUE COMMENT '邮箱',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `avatar` VARCHAR(500) COMMENT '头像URL',
  `nickname` VARCHAR(50) COMMENT '昵称',
  `preferences` JSON COMMENT '偏好标签JSON',
  `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色:USER/ADMIN',
  `status` TINYINT DEFAULT 1 COMMENT '状态:1-正常 0-禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_username` (`username`),
  INDEX `idx_phone` (`phone`),
  INDEX `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ==================== 音乐相关表 ====================

-- 音乐库表
CREATE TABLE IF NOT EXISTS `music` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '音乐ID',
  `title` VARCHAR(200) NOT NULL COMMENT '歌曲名',
  `artist` VARCHAR(100) COMMENT '歌手',
  `album` VARCHAR(200) COMMENT '专辑',
  `file_url` VARCHAR(500) NOT NULL COMMENT '音频文件URL(MinIO)',
  `preview_url` VARCHAR(500) COMMENT '试听片段URL',
  `duration` INT COMMENT '时长(秒)',
  `genre` VARCHAR(50) COMMENT '风格/流派',
  `features` JSON COMMENT 'MFCC等特征向量',
  `cover_url` VARCHAR(500) COMMENT '封面图URL',
  `play_count` INT DEFAULT 0 COMMENT '播放次数',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `status` TINYINT DEFAULT 1 COMMENT '状态:1-正常 0-下架',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_artist` (`artist`),
  INDEX `idx_genre` (`genre`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音乐库表';

-- ==================== 上传与分析相关表 ====================

-- 用户上传记录表
CREATE TABLE IF NOT EXISTS `upload_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '上传ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `file_url` VARCHAR(500) NOT NULL COMMENT '文件URL(MinIO)',
  `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_size` BIGINT COMMENT '文件大小(字节)',
  `file_type` VARCHAR(50) COMMENT '文件类型',
  `status` VARCHAR(20) DEFAULT 'UPLOADING' COMMENT '状态:UPLOADING/ANALYZING/COMPLETED/FAILED',
  `features` JSON COMMENT '提取的音频特征',
  `analysis_result` TEXT COMMENT '分析结果描述',
  `error_message` VARCHAR(500) COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户上传记录表';

-- 推荐记录表
CREATE TABLE IF NOT EXISTS `recommend_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐ID',
  `upload_id` BIGINT NOT NULL COMMENT '上传记录ID',
  `music_id` BIGINT NOT NULL COMMENT '音乐ID',
  `similarity` DECIMAL(5,4) NOT NULL COMMENT '相似度分数(0-1)',
  `rank` INT NOT NULL COMMENT '推荐排名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (`upload_id`) REFERENCES `upload_record`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE,
  INDEX `idx_upload_id` (`upload_id`),
  INDEX `idx_music_id` (`music_id`),
  INDEX `idx_similarity` (`similarity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐记录表';

-- ==================== 反馈与互动相关表 ====================

-- 反馈表
CREATE TABLE IF NOT EXISTS `feedback` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '反馈ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `recommend_id` BIGINT COMMENT '推荐记录ID',
  `music_id` BIGINT COMMENT '音乐ID',
  `type` VARCHAR(20) NOT NULL COMMENT '类型:LIKE/DISLIKE/COMMENT',
  `content` TEXT COMMENT '评论内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`recommend_id`) REFERENCES `recommend_record`(`id`) ON DELETE SET NULL,
  FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE SET NULL,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_type` (`type`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈表';

-- ==================== 管理员相关表 ====================

-- 通知公告表
CREATE TABLE IF NOT EXISTS `notice` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT NOT NULL COMMENT '内容',
  `type` VARCHAR(20) DEFAULT 'SYSTEM' COMMENT '类型:SYSTEM/UPDATE/NEW_MUSIC',
  `status` TINYINT DEFAULT 1 COMMENT '状态:1-发布 0-草稿',
  `publish_time` DATETIME COMMENT '发布时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_status` (`status`),
  INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';

-- 站点内容表（轮播图、热门歌单等）
CREATE TABLE IF NOT EXISTS `site_content` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '内容ID',
  `type` VARCHAR(50) NOT NULL COMMENT '类型:CAROUSEL/HOT_PLAYLIST/BANNER',
  `title` VARCHAR(200) COMMENT '标题',
  `content` TEXT COMMENT '内容JSON',
  `image_url` VARCHAR(500) COMMENT '图片URL',
  `link_url` VARCHAR(500) COMMENT '链接URL',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态:1-启用 0-禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_type` (`type`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点内容表';

-- ==================== 验证码相关表 ====================

-- 验证码表
CREATE TABLE IF NOT EXISTS `verification_code` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
  `code` VARCHAR(10) NOT NULL COMMENT '验证码（6位数字）',
  `type` VARCHAR(20) NOT NULL COMMENT '类型:RESET_PASSWORD',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `is_used` TINYINT DEFAULT 0 COMMENT '是否已使用:0-未使用 1-已使用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_email_code` (`email`, `code`),
  INDEX `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证码表';

-- ==================== 统计相关表 ====================

-- 用户行为统计表（可选，用于数据分析）
CREATE TABLE IF NOT EXISTS `user_behavior` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '行为ID',
  `user_id` BIGINT COMMENT '用户ID',
  `action` VARCHAR(50) NOT NULL COMMENT '行为:VIEW/UPLOAD/PLAY/LIKE',
  `target_type` VARCHAR(50) COMMENT '目标类型:MUSIC/RECOMMEND',
  `target_id` BIGINT COMMENT '目标ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_action` (`action`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为统计表';

-- ==================== 初始化数据 ====================

-- 插入默认管理员账号（密码: admin123，需要BCrypt加密后替换）
-- INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`) 
-- VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJwC', '系统管理员', 'ADMIN', 1);

