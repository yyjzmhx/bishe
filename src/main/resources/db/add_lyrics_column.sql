-- 为 music 表添加 lyrics 字段
ALTER TABLE `music` ADD COLUMN `lyrics` TEXT COMMENT '歌词内容(LRC格式)' AFTER `genre`;

