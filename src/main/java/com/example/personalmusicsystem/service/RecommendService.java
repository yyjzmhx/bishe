package com.example.personalmusicsystem.service;

import com.alibaba.fastjson2.JSON;
import com.example.personalmusicsystem.entity.Music;
import com.example.personalmusicsystem.entity.RecommendRecord;
import com.example.personalmusicsystem.entity.UploadRecord;
import com.example.personalmusicsystem.mapper.MusicMapper;
import com.example.personalmusicsystem.mapper.RecommendRecordMapper;
import com.example.personalmusicsystem.mapper.UploadRecordMapper;
import com.example.personalmusicsystem.service.ai.AudioAnalysisContext;
import com.example.personalmusicsystem.service.ai.AudioAnalysisService;
import com.example.personalmusicsystem.service.ai.DynamicAIChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Recommendation application service.
 */
@Service
@Slf4j
public class RecommendService {

    private static final double RECOMMENDATION_THRESHOLD = 0.35d;

    @Autowired
    private UploadRecordMapper uploadRecordMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private RecommendRecordMapper recommendRecordMapper;

    @Autowired
    private AudioAnalysisService audioAnalysisService;

    @Autowired
    private DynamicAIChatService dynamicAIChatService;

    @Autowired
    private MusicService musicService;

    @Transactional
    public void analyzeAndRecommend(Long uploadId) {
        UploadRecord uploadRecord = uploadRecordMapper.selectById(uploadId);
        if (uploadRecord == null) {
            throw new RuntimeException("上传记录不存在");
        }

        try {
            uploadRecord.setStatus("ANALYZING");
            uploadRecord.setErrorMessage(null);
            uploadRecordMapper.update(uploadRecord);

            String features = audioAnalysisService.extractFeatures(AudioAnalysisContext.fromUploadRecord(uploadRecord));
            uploadRecord.setFeatures(features);

            String recommendationReason = generateRecommendationReason(features);
            uploadRecord.setAnalysisResult(recommendationReason);
            uploadRecord.setStatus("COMPLETED");
            uploadRecordMapper.update(uploadRecord);

            generateRecommendations(uploadId, features);
        } catch (Exception e) {
            log.error("Audio analysis failed, uploadId={}", uploadId, e);
            uploadRecord.setStatus("FAILED");

            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 497) + "...";
            }
            uploadRecord.setErrorMessage(errorMessage);
            uploadRecordMapper.update(uploadRecord);
            throw e;
        }
    }

    private void generateRecommendations(Long uploadId, String uploadFeatures) {
        List<Music> musicList = musicMapper.selectByFeatures(uploadFeatures, 2000);
        if (musicList.isEmpty()) {
            log.warn("No library tracks available for recommendation");
            recommendRecordMapper.deleteByUploadId(uploadId);
            return;
        }

        List<RecommendRecord> recommendRecords = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        for (Music rawMusic : musicList) {
            Music music = ensureFeatures(rawMusic);
            if (music == null || !StringUtils.hasText(music.getFeatures())) {
                skipCount++;
                continue;
            }

            try {
                double similarity = audioAnalysisService.calculateSimilarity(uploadFeatures, music.getFeatures());
                if (similarity < RECOMMENDATION_THRESHOLD) {
                    continue;
                }

                RecommendRecord record = new RecommendRecord();
                record.setUploadId(uploadId);
                record.setMusicId(music.getId());
                record.setSimilarity(BigDecimal.valueOf(similarity));
                record.setMusic(music);
                recommendRecords.add(record);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("Failed to score musicId={}, title={}, error={}",
                        music.getId(), music.getTitle(), e.getMessage());
            }
        }

        List<RecommendRecord> topRecords = recommendRecords.stream()
                .sorted(Comparator.comparing(RecommendRecord::getSimilarity).reversed())
                .limit(20)
                .collect(Collectors.toList());

        for (int index = 0; index < topRecords.size(); index++) {
            topRecords.get(index).setRank(index + 1);
        }

        recommendRecordMapper.deleteByUploadId(uploadId);
        if (!topRecords.isEmpty()) {
            recommendRecordMapper.insertBatch(topRecords);
        }

        log.info("Recommendation completed, uploadId={}, success={}, fail={}, skipped={}, finalCount={}",
                uploadId, successCount, failCount, skipCount, topRecords.size());
    }

    public List<RecommendRecord> getRecommendations(Long uploadId) {
        return recommendRecordMapper.selectByUploadId(uploadId);
    }

    private Music ensureFeatures(Music music) {
        if (music == null) {
            return null;
        }
        if (StringUtils.hasText(music.getFeatures())) {
            return music;
        }
        return musicService.refreshAudioFeatures(music);
    }

    private String generateRecommendationReason(String features) {
        String fallbackReason = buildFallbackRecommendationReason(features);
        if (!dynamicAIChatService.hasUsableRuntimeConfig()) {
            return fallbackReason;
        }

        String prompt = String.format("""
                基于以下结构化音频分析结果，写一句简洁的推荐理由，限制在 60 字以内。
                要求：只返回自然语言，不要编号，不要解释模型过程。

                分析结果:
                %s
                """, features);

        try {
            String reason = dynamicAIChatService.prompt(prompt);
            reason = reason == null ? "" : reason.trim();
            if (!StringUtils.hasText(reason)) {
                return fallbackReason;
            }
            if (reason.length() > 80) {
                reason = reason.substring(0, 80) + "...";
            }
            return reason;
        } catch (Exception e) {
            log.warn("Failed to generate AI recommendation reason: {}", e.getMessage());
            return fallbackReason;
        }
    }

    private String buildFallbackRecommendationReason(String features) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> featureMap = JSON.parseObject(features, Map.class);
            if (featureMap == null || featureMap.isEmpty()) {
                return "系统已基于本地音频特征完成匹配，优先推荐节奏和氛围相近的曲目。";
            }

            String style = asString(featureMap.get("style"));
            String emotion = asString(featureMap.get("emotion"));
            String rhythm = asString(featureMap.get("rhythm"));
            String atmosphere = asString(featureMap.get("atmosphere"));

            StringBuilder builder = new StringBuilder("系统优先匹配");
            if (StringUtils.hasText(style)) {
                builder.append(style);
            } else {
                builder.append("相近风格");
            }
            if (StringUtils.hasText(emotion)) {
                builder.append("、").append(emotion);
            }
            if (StringUtils.hasText(rhythm)) {
                builder.append("与").append(rhythm);
            }
            if (StringUtils.hasText(atmosphere)) {
                builder.append("的").append(atmosphere).append("曲目");
            } else {
                builder.append("的曲目");
            }
            builder.append("。");
            return builder.toString();
        } catch (Exception e) {
            log.debug("Fallback recommendation reason parsing failed", e);
            return "系统已基于本地音频特征完成匹配，优先推荐节奏和氛围相近的曲目。";
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
