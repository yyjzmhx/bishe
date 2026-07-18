package com.example.personalmusicsystem.controller;

import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.Music;
import com.example.personalmusicsystem.service.MusicService;
import com.example.personalmusicsystem.service.PlaylistService;
import com.example.personalmusicsystem.service.PlaylistService.PlayState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 播放列表控制器
 */
@RestController
@RequestMapping("/api/user/playlist")
@Slf4j
public class PlaylistController {
    
    @Autowired
    private PlaylistService playlistService;
    
    @Autowired
    private MusicService musicService;
    
    /**
     * 保存播放列表
     */
    @PostMapping("/save")
    public Result<String> savePlaylist(@RequestAttribute Long userId,
                                       @RequestBody List<Long> musicIds) {
        try {
            // 根据ID获取完整的音乐信息
            List<Music> playlist = musicIds.stream()
                    .map(id -> musicService.getById(id))
                    .filter(music -> music != null)
                    .toList();
            
            playlistService.savePlaylist(userId, playlist);
            return Result.success("保存播放列表成功");
        } catch (Exception e) {
            log.error("保存播放列表失败", e);
            return Result.error("保存播放列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取播放列表
     */
    @GetMapping("/get")
    public Result<List<Music>> getPlaylist(@RequestAttribute Long userId) {
        try {
            List<Music> playlist = playlistService.getPlaylist(userId);
            return Result.success(playlist);
        } catch (Exception e) {
            log.error("获取播放列表失败", e);
            return Result.error("获取播放列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存播放状态
     */
    @PostMapping("/state/save")
    public Result<String> savePlayState(@RequestAttribute Long userId,
                                        @RequestBody PlayStateRequest request) {
        try {
            playlistService.savePlayState(
                    userId,
                    request.getCurrentMusicId(),
                    request.getCurrentIndex(),
                    request.getPlayMode(),
                    request.getCurrentTime(),
                    request.getIsPlaying()
            );
            return Result.success("保存播放状态成功");
        } catch (Exception e) {
            log.error("保存播放状态失败", e);
            return Result.error("保存播放状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取播放状态
     */
    @GetMapping("/state/get")
    public Result<Map<String, Object>> getPlayState(@RequestAttribute Long userId) {
        try {
            PlayState state = playlistService.getPlayState(userId);
            if (state == null) {
                return Result.success(null);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("currentMusicId", state.getCurrentMusicId());
            result.put("currentIndex", state.getCurrentIndex());
            result.put("playMode", state.getPlayMode());
            result.put("currentTime", state.getCurrentTime());
            result.put("isPlaying", state.getIsPlaying());
            
            // 如果有关联的音乐ID，返回完整的音乐信息
            if (state.getCurrentMusicId() != null) {
                Music music = musicService.getById(state.getCurrentMusicId());
                result.put("currentMusic", music);
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取播放状态失败", e);
            return Result.error("获取播放状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 清除播放数据（退出登录时调用）
     */
    @PostMapping("/clear")
    public Result<String> clearPlaylist(@RequestAttribute Long userId) {
        try {
            playlistService.clearUserData(userId);
            return Result.success("清除播放数据成功");
        } catch (Exception e) {
            log.error("清除播放数据失败", e);
            return Result.error("清除播放数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 播放状态请求DTO
     */
    @Data
    public static class PlayStateRequest {
        private Long currentMusicId;
        private Integer currentIndex;
        private String playMode;
        private Double currentTime;
        private Boolean isPlaying;
    }
}

