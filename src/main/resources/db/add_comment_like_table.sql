-- 评论点赞表
CREATE TABLE IF NOT EXISTS `comment_like` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
  `comment_id` BIGINT NOT NULL COMMENT '评论ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
  FOREIGN KEY (`comment_id`) REFERENCES `feedback`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  INDEX `idx_comment_id` (`comment_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

