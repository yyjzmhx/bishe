-- 添加parent_id字段支持评论回复
ALTER TABLE `feedback` ADD COLUMN `parent_id` BIGINT COMMENT '父评论ID（回复时使用）' AFTER `music_id`;
ALTER TABLE `feedback` ADD INDEX `idx_parent_id` (`parent_id`);
ALTER TABLE `feedback` ADD FOREIGN KEY (`parent_id`) REFERENCES `feedback`(`id`) ON DELETE CASCADE;

