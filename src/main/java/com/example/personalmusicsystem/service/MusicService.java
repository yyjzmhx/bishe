package com.example.personalmusicsystem.service;

import com.example.personalmusicsystem.entity.Music;
import com.example.personalmusicsystem.mapper.MusicMapper;
import com.example.personalmusicsystem.service.ai.AudioAnalysisContext;
import com.example.personalmusicsystem.service.ai.AudioAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Music application service.
 */
@Service
@Slf4j
public class MusicService {

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private AudioAnalysisService audioAnalysisService;

    public Music getById(Long id) {
        return musicMapper.selectById(id);
    }

    public Map<String, Object> getMusicList(String keyword,
                                            String artist,
                                            String genre,
                                            Integer status,
                                            int pageNum,
                                            int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Music> list = musicMapper.selectList(keyword, artist, genre, status, offset, pageSize);
        long total = musicMapper.count(keyword, artist, genre, status);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    public boolean addMusic(Music music) {
        if (music.getStatus() == null) {
            music.setStatus(1);
        }

        log.info("Insert music, title={}, artist={}, fileUrl={}",
                music.getTitle(), music.getArtist(), music.getFileUrl());
        int result = musicMapper.insert(music);
        if (result > 0 && music.getId() != null) {
            refreshAudioFeatures(music);
        }
        return result > 0;
    }

    public boolean updateMusic(Music music) {
        if (music == null || music.getId() == null) {
            log.warn("Skip music update because payload or id is null");
            return false;
        }

        int affectedRows = musicMapper.update(music);
        boolean success = affectedRows > 0;
        if (success) {
            log.info("Updated music, musicId={}", music.getId());
        } else {
            log.warn("Music update affected no rows, musicId={}", music.getId());
        }
        return success;
    }

    /**
     * Regenerate structured features for a library track.
     */
    public Music refreshAudioFeatures(Long musicId) {
        Music music = musicMapper.selectById(musicId);
        if (music == null) {
            return null;
        }
        return refreshAudioFeatures(music);
    }

    /**
     * Regenerate structured features for a loaded music entity.
     */
    public Music refreshAudioFeatures(Music music) {
        if (music == null || music.getId() == null || !StringUtils.hasText(music.getFileUrl())) {
            return music;
        }

        try {
            String features = audioAnalysisService.extractFeatures(AudioAnalysisContext.fromMusic(music));

            Music updatePayload = new Music();
            updatePayload.setId(music.getId());
            updatePayload.setFeatures(features);

            Integer durationSeconds = audioAnalysisService.extractDurationSeconds(features);
            if (durationSeconds != null && durationSeconds > 0) {
                updatePayload.setDuration(durationSeconds);
            }

            if (!StringUtils.hasText(music.getGenre())) {
                String styleLabel = audioAnalysisService.extractStyleLabel(features);
                if (StringUtils.hasText(styleLabel)) {
                    updatePayload.setGenre(styleLabel);
                }
            }

            musicMapper.update(updatePayload);
            Music refreshedMusic = musicMapper.selectById(music.getId());
            log.info("Refreshed music features, musicId={}", music.getId());
            return refreshedMusic == null ? music : refreshedMusic;
        } catch (Exception e) {
            log.warn("Failed to refresh music features, musicId={}, error={}", music.getId(), e.getMessage());
            return music;
        }
    }

    public boolean deleteMusic(Long id) {
        return musicMapper.deleteById(id) > 0;
    }

    public void incrementPlayCount(Long id) {
        Music music = musicMapper.selectById(id);
        if (music != null) {
            music.setPlayCount((music.getPlayCount() == null ? 0 : music.getPlayCount()) + 1);
            musicMapper.update(music);
        }
    }
}
