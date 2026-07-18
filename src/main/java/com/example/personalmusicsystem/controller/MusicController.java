package com.example.personalmusicsystem.controller;

import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.Music;
import com.example.personalmusicsystem.service.MusicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 音乐控制器
 */
@RestController
@RequestMapping("/api/user")
@Slf4j
public class MusicController {
    
    @Autowired
    private MusicService musicService;
    
    /**
     * 获取音乐详情
     */
    @GetMapping("/music/{id}")
    public Result<Music> getMusic(@PathVariable Long id) {
        Music music = musicService.getById(id);
        if (music == null) {
            return Result.error("音乐不存在");
        }
        return Result.success(music);
    }
    
    /**
     * 获取音乐列表
     */
    @GetMapping("/music/list")
    public Result<Map<String, Object>> getMusicList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        Map<String, Object> result = musicService.getMusicList(
            keyword, artist, genre, 1, pageNum, pageSize
        );
        return Result.success(result);
    }
    
    /**
     * 获取试听片段URL
     */
    @GetMapping("/music/preview/{id}")
    public Result<Map<String, String>> getPreviewUrl(@PathVariable Long id) {
        Music music = musicService.getById(id);
        if (music == null) {
            return Result.error("音乐不存在");
        }
        
        Map<String, String> result = new java.util.HashMap<>();
        result.put("previewUrl", music.getPreviewUrl());
        return Result.success(result);
    }
}

