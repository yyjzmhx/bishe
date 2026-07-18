-- ==================== 测试数据 ====================
-- 注意：执行此脚本前请先执行 schema.sql 创建表结构

USE music_system;

-- ==================== 测试用户数据 ====================
-- 注意：密码已使用BCrypt加密
-- 管理员账号: admin / admin123
-- 普通用户: user001, user002, user003 / user123

-- 管理员账号（密码: admin123）
-- 使用BCrypt加密，可以通过运行 PasswordGenerator.java 生成新的哈希值
INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `email`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJwC', '系统管理员', '13800138000', 'admin@music.com', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE `password`=VALUES(`password`);

-- 普通用户（密码: user123）
INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `email`, `role`, `status`) VALUES
('user001', '$2a$10$7xKXqJYzQZvLmNpRwTfH.eXzYqWvBnMjKlOpQrStUvWxYzAbCdEf', '音乐爱好者', '13800138001', 'user001@music.com', 'USER', 1),
('user002', '$2a$10$7xKXqJYzQZvLmNpRwTfH.eXzYqWvBnMjKlOpQrStUvWxYzAbCdEf', '旋律控', '13800138002', 'user002@music.com', 'USER', 1),
('user003', '$2a$10$7xKXqJYzQZvLmNpRwTfH.eXzYqWvBnMjKlOpQrStUvWxYzAbCdEf', '节奏大师', '13800138003', 'user003@music.com', 'USER', 1)
ON DUPLICATE KEY UPDATE `password`=VALUES(`password`);

-- ==================== 测试音乐数据 ====================

INSERT INTO `music` (`title`, `artist`, `album`, `file_url`, `preview_url`, `duration`, `genre`, `features`, `cover_url`, `play_count`, `like_count`, `status`) VALUES
('夜空中最亮的星', '逃跑计划', '世界', 'http://localhost:9000/music-storage/library/music_1.mp3', 'http://localhost:9000/music-storage/previews/preview_1.mp3', 285, '流行', '[0.75, 0.68, 0.72, 0.65, 0.70, 0.73, 0.69, 0.71]', 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300', 1250, 89, 1),
('成都', '赵雷', '无法长大', 'http://localhost:9000/music-storage/library/music_2.mp3', 'http://localhost:9000/music-storage/previews/preview_2.mp3', 320, '民谣', '[0.68, 0.72, 0.65, 0.70, 0.66, 0.74, 0.67, 0.69]', 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=300', 2100, 156, 1),
('海阔天空', 'Beyond', '乐与怒', 'http://localhost:9000/music-storage/library/music_3.mp3', 'http://localhost:9000/music-storage/previews/preview_3.mp3', 268, '摇滚', '[0.82, 0.75, 0.78, 0.80, 0.85, 0.77, 0.79, 0.83]', 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300', 3500, 298, 1),
('告白气球', '周杰伦', '周杰伦的床边故事', 'http://localhost:9000/music-storage/library/music_4.mp3', 'http://localhost:9000/music-storage/previews/preview_4.mp3', 245, '流行', '[0.70, 0.73, 0.68, 0.72, 0.69, 0.71, 0.74, 0.70]', 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300', 1890, 134, 1),
('起风了', '买辣椒也用券', '起风了', 'http://localhost:9000/music-storage/library/music_5.mp3', 'http://localhost:9000/music-storage/previews/preview_5.mp3', 310, '流行', '[0.72, 0.70, 0.74, 0.68, 0.71, 0.73, 0.69, 0.72]', 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=300', 1650, 112, 1),
('演员', '薛之谦', '绅士', 'http://localhost:9000/music-storage/library/music_6.mp3', 'http://localhost:9000/music-storage/previews/preview_6.mp3', 275, '流行', '[0.69, 0.71, 0.67, 0.73, 0.70, 0.72, 0.68, 0.71]', 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300', 1420, 98, 1),
('像我这样的人', '毛不易', '平凡的一天', 'http://localhost:9000/music-storage/library/music_7.mp3', 'http://localhost:9000/music-storage/previews/preview_7.mp3', 295, '民谣', '[0.66, 0.69, 0.64, 0.71, 0.67, 0.70, 0.65, 0.68]', 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=300', 980, 76, 1),
('晴天', '周杰伦', '叶惠美', 'http://localhost:9000/music-storage/library/music_8.mp3', 'http://localhost:9000/music-storage/previews/preview_8.mp3', 280, '流行', '[0.71, 0.74, 0.69, 0.72, 0.70, 0.73, 0.71, 0.72]', 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300', 2300, 187, 1),
('理想', '赵雷', '无法长大', 'http://localhost:9000/music-storage/library/music_9.mp3', 'http://localhost:9000/music-storage/previews/preview_9.mp3', 305, '民谣', '[0.67, 0.70, 0.65, 0.72, 0.68, 0.71, 0.66, 0.69]', 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=300', 1100, 85, 1),
('七里香', '周杰伦', '七里香', 'http://localhost:9000/music-storage/library/music_10.mp3', 'http://localhost:9000/music-storage/previews/preview_10.mp3', 298, '流行', '[0.73, 0.71, 0.75, 0.69, 0.72, 0.74, 0.70, 0.73]', 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300', 1980, 145, 1),
('南山南', '马頔', '孤岛', 'http://localhost:9000/music-storage/library/music_11.mp3', 'http://localhost:9000/music-storage/previews/preview_11.mp3', 312, '民谣', '[0.65, 0.68, 0.63, 0.70, 0.66, 0.69, 0.64, 0.67]', 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=300', 890, 67, 1),
('追光者', '岑宁儿', '夏至未至 电视剧原声带', 'http://localhost:9000/music-storage/library/music_12.mp3', 'http://localhost:9000/music-storage/previews/preview_12.mp3', 265, '流行', '[0.74, 0.72, 0.76, 0.68, 0.71, 0.75, 0.69, 0.74]', 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300', 1750, 128, 1);

-- ==================== 测试上传记录 ====================

INSERT INTO `upload_record` (`user_id`, `file_url`, `file_name`, `file_size`, `file_type`, `status`, `features`, `analysis_result`) VALUES
(2, 'http://localhost:9000/music-storage/uploads/2024/12/28/user_2_test1.mp3', 'test_music_1.mp3', 5242880, 'mp3', 'COMPLETED', '[0.72, 0.70, 0.74, 0.68, 0.71, 0.73, 0.69, 0.72]', '音频特征提取成功，节奏明快，旋律优美，适合推荐相似风格的流行音乐。'),
(2, 'http://localhost:9000/music-storage/uploads/2024/12/28/user_2_test2.mp3', 'test_music_2.mp3', 6291456, 'mp3', 'COMPLETED', '[0.68, 0.72, 0.65, 0.70, 0.66, 0.74, 0.67, 0.69]', '民谣风格明显，情感表达丰富，推荐相似民谣作品。'),
(3, 'http://localhost:9000/music-storage/uploads/2024/12/27/user_3_test1.mp3', 'rock_music.mp3', 7340032, 'mp3', 'COMPLETED', '[0.82, 0.75, 0.78, 0.80, 0.85, 0.77, 0.79, 0.83]', '摇滚风格强烈，能量充沛，节奏感强。'),
(3, 'http://localhost:9000/music-storage/uploads/2024/12/27/user_3_test2.mp3', 'pop_music.mp3', 4194304, 'mp3', 'ANALYZING', NULL, NULL),
(4, 'http://localhost:9000/music-storage/uploads/2024/12/26/user_4_test1.mp3', 'folk_music.mp3', 5767168, 'mp3', 'COMPLETED', '[0.66, 0.69, 0.64, 0.71, 0.67, 0.70, 0.65, 0.68]', '民谣风格，旋律简单优美。');

-- ==================== 测试推荐记录 ====================

-- 为上传记录ID=1生成推荐
INSERT INTO `recommend_record` (`upload_id`, `music_id`, `similarity`, `rank`) VALUES
(1, 4, 0.92, 1),
(1, 5, 0.88, 2),
(1, 6, 0.85, 3),
(1, 8, 0.83, 4),
(1, 10, 0.81, 5),
(1, 12, 0.79, 6);

-- 为上传记录ID=2生成推荐
INSERT INTO `recommend_record` (`upload_id`, `music_id`, `similarity`, `rank`) VALUES
(2, 2, 0.94, 1),
(2, 7, 0.91, 2),
(2, 9, 0.89, 3),
(2, 11, 0.87, 4);

-- 为上传记录ID=3生成推荐
INSERT INTO `recommend_record` (`upload_id`, `music_id`, `similarity`, `rank`) VALUES
(3, 3, 0.96, 1),
(3, 1, 0.82, 2),
(3, 4, 0.80, 3);

-- 为上传记录ID=5生成推荐
INSERT INTO `recommend_record` (`upload_id`, `music_id`, `similarity`, `rank`) VALUES
(5, 2, 0.93, 1),
(5, 7, 0.90, 2),
(5, 9, 0.88, 3),
(5, 11, 0.86, 4);

-- ==================== 测试反馈数据 ====================

INSERT INTO `feedback` (`user_id`, `recommend_id`, `music_id`, `type`, `content`) VALUES
(2, 1, 4, 'LIKE', NULL),
(2, 2, 5, 'LIKE', NULL),
(2, 3, 6, 'COMMENT', '这首歌很不错，推荐很准确！'),
(3, 7, 3, 'LIKE', NULL),
(3, 8, 1, 'DISLIKE', NULL),
(4, 13, 2, 'LIKE', NULL);

-- ==================== 测试通知公告 ====================

INSERT INTO `notice` (`title`, `content`, `type`, `status`, `publish_time`) VALUES
('系统更新通知', '系统已完成升级，新增了音频分析功能，欢迎体验！', 'SYSTEM', 1, NOW()),
('新功能上线', 'AI推荐算法已优化，推荐准确度提升30%！', 'UPDATE', 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('新歌推荐', '本周新增了10首热门歌曲，快来听听吧！', 'NEW_MUSIC', 1, DATE_SUB(NOW(), INTERVAL 5 DAY));

-- ==================== 测试站点内容 ====================

INSERT INTO `site_content` (`type`, `title`, `content`, `image_url`, `link_url`, `sort_order`, `status`) VALUES
('CAROUSEL', 'AI智能推荐', '上传你的音乐，AI为你找到相似的音乐', 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1920', '/upload', 1, 1),
('CAROUSEL', '个性化体验', '基于你的喜好，推荐最适合的音乐', 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=1920', '/upload', 2, 1),
('CAROUSEL', '发现新音乐', '探索音乐库，发现更多精彩', 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1920', '/', 3, 1),
('HOT_PLAYLIST', '热门推荐', '{"songs": [1, 3, 4, 8]}', NULL, '/', 1, 1),
('HOT_PLAYLIST', '民谣精选', '{"songs": [2, 7, 9, 11]}', NULL, '/', 2, 1);

-- ==================== 测试用户行为数据 ====================

INSERT INTO `user_behavior` (`user_id`, `action`, `target_type`, `target_id`) VALUES
(2, 'UPLOAD', 'UPLOAD', 1),
(2, 'PLAY', 'MUSIC', 4),
(2, 'LIKE', 'MUSIC', 4),
(3, 'UPLOAD', 'UPLOAD', 3),
(3, 'PLAY', 'MUSIC', 3),
(4, 'UPLOAD', 'UPLOAD', 5),
(4, 'VIEW', 'MUSIC', 2);

