package com.example.personalmusicsystem.service.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioAnalysisServiceTest {

    private final AudioAnalysisService audioAnalysisService = new AudioAnalysisService();

    @Test
    void shouldGiveHighSimilarityForSameStructuredFeatures() {
        String features = """
                {
                  "style": "电子/流行",
                  "emotion": "兴奋/积极",
                  "rhythm": "节奏推进明显",
                  "atmosphere": "明亮外放",
                  "description": "整体能量较高，节奏推进明显，音色偏明亮。",
                  "vector": [0.82, 0.78, 0.74, 0.55, 0.60]
                }
                """;

        double similarity = audioAnalysisService.calculateSimilarity(features, features);
        assertTrue(similarity > 0.90d);
    }

    @Test
    void shouldDistinguishEnergeticAndCalmTracks() {
        String energetic = """
                {
                  "style": "电子/流行",
                  "emotion": "兴奋/积极",
                  "rhythm": "节奏推进明显",
                  "atmosphere": "明亮外放",
                  "description": "高能量、强律动、偏明亮。",
                  "vector": [0.88, 0.82, 0.72, 0.48, 0.52]
                }
                """;

        String calm = """
                {
                  "style": "轻音乐/环境",
                  "emotion": "平静/沉浸",
                  "rhythm": "节奏舒缓",
                  "atmosphere": "安静沉浸",
                  "description": "能量较低、节奏舒缓、氛围安静。",
                  "vector": [0.22, 0.18, 0.35, 0.62, 0.41]
                }
                """;

        double similarity = audioAnalysisService.calculateSimilarity(energetic, calm);
        assertTrue(similarity < 0.65d);
    }

    @Test
    void shouldExtractDurationFromStructuredFeatures() {
        String features = """
                {
                  "durationSeconds": 245,
                  "vector": [0.51, 0.49, 0.45, 0.58, 0.55]
                }
                """;

        assertEquals(245, audioAnalysisService.extractDurationSeconds(features));
    }
}
