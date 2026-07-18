package com.example.personalmusicsystem.controller.admin;

import com.example.personalmusicsystem.annotation.RequireAdmin;
import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.AIConfig;
import com.example.personalmusicsystem.mapper.AIConfigMapper;
import com.example.personalmusicsystem.mapper.FeedbackMapper;
import com.example.personalmusicsystem.mapper.MusicMapper;
import com.example.personalmusicsystem.mapper.RecommendRecordMapper;
import com.example.personalmusicsystem.mapper.UploadRecordMapper;
import com.example.personalmusicsystem.service.ai.DynamicAIChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin AI monitor endpoints.
 */
@RestController
@RequestMapping("/api/admin/ai")
@RequireAdmin
@Slf4j
public class AdminAIController {

    @Autowired
    private RecommendRecordMapper recommendRecordMapper;

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private UploadRecordMapper uploadRecordMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private AIConfigMapper aiConfigMapper;

    @Autowired
    private DynamicAIChatService dynamicAIChatService;

    /**
     * Recommendation quality overview.
     */
    @GetMapping("/accuracy")
    public Result<Map<String, Object>> getAccuracyStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Map<String, Object> stats = new HashMap<>();
        Double avgSimilarityValue = recommendRecordMapper.selectAverageSimilarity();
        double avgSimilarity = avgSimilarityValue == null ? 0.0 : avgSimilarityValue;
        long totalRecommends = recommendRecordMapper.countAll();
        long positiveFeedback = feedbackMapper.count(null, null, "LIKE");
        long negativeFeedback = feedbackMapper.count(null, null, "DISLIKE");
        long evaluatedFeedback = positiveFeedback + negativeFeedback;

        double accuracy = evaluatedFeedback > 0
                ? positiveFeedback * 100.0 / evaluatedFeedback
                : avgSimilarity * 100.0;

        stats.put("accuracy", Math.min(Math.max(accuracy, 0.0), 100.0));
        stats.put("avgSimilarity", avgSimilarity);
        stats.put("totalRecommends", totalRecommends);
        stats.put("positiveFeedback", positiveFeedback);
        stats.put("negativeFeedback", negativeFeedback);
        stats.put("evaluatedFeedback", evaluatedFeedback);
        stats.put("startDate", startDate);
        stats.put("endDate", endDate);

        return Result.success(stats);
    }

    /**
     * Music genre distribution.
     */
    @GetMapping("/features/distribution")
    public Result<Map<String, Object>> getFeatureDistribution() {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> genreDistribution = musicMapper.selectGenreDistribution(8);

        Map<String, Integer> distribution = new HashMap<>();
        for (Map<String, Object> item : genreDistribution) {
            String name = String.valueOf(item.getOrDefault("name", "未分类"));
            Number value = (Number) item.get("value");
            distribution.put(name, value == null ? 0 : value.intValue());
        }

        data.put("distribution", distribution);
        return Result.success(data);
    }

    /**
     * Feedback summary used by the AI monitor dashboard.
     */
    @GetMapping("/feedback/aggregation")
    public Result<Map<String, Object>> getFeedbackAggregation() {
        Map<String, Object> data = new HashMap<>();

        long likeCount = feedbackMapper.count(null, null, "LIKE");
        long dislikeCount = feedbackMapper.count(null, null, "DISLIKE");
        long commentCount = feedbackMapper.count(null, null, "COMMENT");
        long favoriteCount = feedbackMapper.count(null, null, "FAVORITE");
        long activeUsers = feedbackMapper.countDistinctUsersByTypes("LIKE,DISLIKE,COMMENT,FAVORITE");

        data.put("likeCount", likeCount);
        data.put("dislikeCount", dislikeCount);
        data.put("commentCount", commentCount);
        data.put("favoriteCount", favoriteCount);
        data.put("activeUsers", activeUsers);
        data.put("totalFeedback", likeCount + dislikeCount + commentCount + favoriteCount);

        return Result.success(data);
    }

    /**
     * Current AI service status.
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getAIStatus() {
        Map<String, Object> status = new HashMap<>();
        AIConfig activeConfig = aiConfigMapper.selectActive();
        AIConfig runtimeConfig = dynamicAIChatService.hasUsableRuntimeConfig()
                ? dynamicAIChatService.getRuntimeConfig()
                : null;

        status.put("online", runtimeConfig != null);
        status.put("analyzingCount", uploadRecordMapper.countByStatus("ANALYZING"));
        status.put("configured", activeConfig != null);
        status.put("configSource", activeConfig == null ? "application" : "database");
        status.put("provider", runtimeConfig == null ? null : runtimeConfig.getProvider());
        status.put("model", runtimeConfig == null ? null : runtimeConfig.getModel());
        status.put("strategy", runtimeConfig == null ? null : runtimeConfig.getAnalysisStrategy());
        status.put("lastCheckTime", System.currentTimeMillis());

        return Result.success(status);
    }
}
