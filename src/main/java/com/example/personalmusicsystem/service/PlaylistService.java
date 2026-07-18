package com.example.personalmusicsystem.service;

import com.alibaba.fastjson2.JSON;
import com.example.personalmusicsystem.entity.Music;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 播放列表服务
 * 使用Redis存储用户的播放列表和播放状态
 */
@Service
@Slf4j
public class PlaylistService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Redis Key前缀
    private static final String PLAYLIST_KEY_PREFIX = "playlist:user:";
    private static final String PLAY_STATE_KEY_PREFIX = "playstate:user:";
    
    // 过期时间：30天
    private static final long EXPIRE_DAYS = 30;
    
    /**
     * 保存播放列表
     */
    public void savePlaylist(Long userId, List<Music> playlist) {
        try {
            String key = PLAYLIST_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(key, playlist, EXPIRE_DAYS, TimeUnit.DAYS);
            log.info("保存播放列表成功，用户ID: {}, 歌曲数量: {}", userId, playlist.size());
        } catch (Exception e) {
            log.error("保存播放列表失败，用户ID: {}", userId, e);
            throw new RuntimeException("保存播放列表失败", e);
        }
    }
    
    /**
     * 获取播放列表
     */
    public List<Music> getPlaylist(Long userId) {
        try {
            String key = PLAYLIST_KEY_PREFIX + userId;
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return new ArrayList<>();
            }
            
            // 处理不同的数据类型
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                List<Music> result = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Music) {
                        result.add((Music) item);
                    } else {
                        // 如果是Map或其他类型，转换为Music对象
                        String jsonStr = JSON.toJSONString(item);
                        Music music = JSON.parseObject(jsonStr, Music.class);
                        result.add(music);
                    }
                }
                return result;
            }
            
            // 如果是JSON字符串，解析
            if (value instanceof String) {
                return JSON.parseArray((String) value, Music.class);
            }
            
            // 尝试直接解析
            try {
                String jsonStr = JSON.toJSONString(value);
                return JSON.parseArray(jsonStr, Music.class);
            } catch (Exception e) {
                log.warn("无法解析播放列表数据，用户ID: {}", userId, e);
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取播放列表失败，用户ID: {}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 保存播放状态
     */
    public void savePlayState(Long userId, Long currentMusicId, Integer currentIndex, 
                              String playMode, Double currentTime, Boolean isPlaying) {
        try {
            String key = PLAY_STATE_KEY_PREFIX + userId;
            PlayState state = new PlayState();
            state.setCurrentMusicId(currentMusicId);
            state.setCurrentIndex(currentIndex);
            state.setPlayMode(playMode);
            state.setCurrentTime(currentTime);
            state.setIsPlaying(isPlaying);
            
            redisTemplate.opsForValue().set(key, state, EXPIRE_DAYS, TimeUnit.DAYS);
            log.debug("保存播放状态成功，用户ID: {}, 当前音乐ID: {}", userId, currentMusicId);
        } catch (Exception e) {
            log.error("保存播放状态失败，用户ID: {}", userId, e);
            throw new RuntimeException("保存播放状态失败", e);
        }
    }
    
    /**
     * 获取播放状态
     */
    public PlayState getPlayState(Long userId) {
        try {
            String key = PLAY_STATE_KEY_PREFIX + userId;
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            
            // 处理不同的数据类型
            if (value instanceof PlayState) {
                return (PlayState) value;
            }
            
            // 如果是Map或JSON字符串，转换为PlayState对象
            return JSON.parseObject(JSON.toJSONString(value), PlayState.class);
        } catch (Exception e) {
            log.error("获取播放状态失败，用户ID: {}", userId, e);
            return null;
        }
    }
    
    /**
     * 清除用户的所有播放数据（退出登录时调用）
     */
    public void clearUserData(Long userId) {
        try {
            String playlistKey = PLAYLIST_KEY_PREFIX + userId;
            String playStateKey = PLAY_STATE_KEY_PREFIX + userId;
            
            redisTemplate.delete(playlistKey);
            redisTemplate.delete(playStateKey);
            
            log.info("清除用户播放数据成功，用户ID: {}", userId);
        } catch (Exception e) {
            log.error("清除用户播放数据失败，用户ID: {}", userId, e);
        }
    }
    
    /**
     * 播放状态内部类
     */
    public static class PlayState {
        private Long currentMusicId;
        private Integer currentIndex;
        private String playMode;
        private Double currentTime;
        private Boolean isPlaying;
        
        public Long getCurrentMusicId() {
            return currentMusicId;
        }
        
        public void setCurrentMusicId(Long currentMusicId) {
            this.currentMusicId = currentMusicId;
        }
        
        public Integer getCurrentIndex() {
            return currentIndex;
        }
        
        public void setCurrentIndex(Integer currentIndex) {
            this.currentIndex = currentIndex;
        }
        
        public String getPlayMode() {
            return playMode;
        }
        
        public void setPlayMode(String playMode) {
            this.playMode = playMode;
        }
        
        public Double getCurrentTime() {
            return currentTime;
        }
        
        public void setCurrentTime(Double currentTime) {
            this.currentTime = currentTime;
        }
        
        public Boolean getIsPlaying() {
            return isPlaying;
        }
        
        public void setIsPlaying(Boolean isPlaying) {
            this.isPlaying = isPlaying;
        }
    }
}

