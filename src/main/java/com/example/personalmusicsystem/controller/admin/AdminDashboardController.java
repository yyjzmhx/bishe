package com.example.personalmusicsystem.controller.admin;

import com.example.personalmusicsystem.annotation.RequireAdmin;
import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员仪表盘控制器
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequireAdmin
@Slf4j
public class AdminDashboardController {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private MusicMapper musicMapper;
    
    @Autowired
    private UploadRecordMapper uploadRecordMapper;
    
    @Autowired
    private FeedbackMapper feedbackMapper;
    
    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 用户统计
        long totalUsers = userMapper.count(null, null, null);
        long activeUsers = userMapper.count(null, null, 1);
        
        // 音乐统计
        long totalMusic = musicMapper.count(null, null, null, null);
        long activeMusic = musicMapper.count(null, null, null, 1);
        
        // 上传统计
        long totalUploads = uploadRecordMapper.countAll();
        
        // 反馈统计
        long totalFeedback = feedbackMapper.count(null, null, null);
        
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalMusic", totalMusic);
        stats.put("activeMusic", activeMusic);
        stats.put("totalUploads", totalUploads);
        stats.put("totalFeedback", totalFeedback);
        
        return Result.success(stats);
    }
    
    /**
     * 获取图表数据
     * @param type 图表类型：userGrowth, genreDistribution, uploadTrend, feedbackType
     * @param period 时间周期：daily, weekly, monthly
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     */
    @GetMapping("/charts")
    public Result<Map<String, Object>> getChartData(
            @RequestParam(required = false, defaultValue = "userGrowth") String type,
            @RequestParam(required = false, defaultValue = "monthly") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        Map<String, Object> chartData = new HashMap<>();
        
        switch (type) {
            case "userGrowth":
                chartData = getUserGrowthData(period, startDate, endDate);
                break;
            case "genreDistribution":
                chartData = getGenreDistributionData();
                break;
            case "uploadTrend":
                chartData = getUploadTrendData(period, startDate, endDate);
                break;
            case "feedbackType":
                chartData = getFeedbackTypeData();
                break;
            default:
                chartData = getUserGrowthData(period, startDate, endDate);
        }
        
        return Result.success(chartData);
    }
    
    /**
     * 获取用户增长趋势数据
     */
    private Map<String, Object> getUserGrowthData(String period, String startDate, String endDate) {
        Map<String, Object> data = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        
        // 简化实现：返回最近6个月的数据
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        
        for (int i = 5; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            dates.add(date.format(formatter));
            // 这里应该查询数据库获取实际数据，暂时使用模拟数据
            values.add((long) (Math.random() * 100 + 50));
        }
        
        data.put("dates", dates);
        data.put("values", values);
        return data;
    }
    
    /**
     * 获取音乐类型分布数据
     */
    private Map<String, Object> getGenreDistributionData() {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> series = new ArrayList<>();
        
        // 查询各类型的音乐数量
        String[] genres = {"流行", "摇滚", "电子", "爵士", "古典", "民谣", "说唱", "R&B"};
        
        for (String genre : genres) {
            long count = musicMapper.count(null, null, genre, 1);
            if (count > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", genre);
                item.put("value", count);
                series.add(item);
            }
        }
        
        data.put("series", series);
        return data;
    }
    
    /**
     * 获取上传趋势数据
     */
    private Map<String, Object> getUploadTrendData(String period, String startDate, String endDate) {
        Map<String, Object> data = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        
        // 简化实现：返回最近6个月的数据
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        
        for (int i = 5; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            dates.add(date.format(formatter));
            // 这里应该查询数据库获取实际数据，暂时使用模拟数据
            values.add((long) (Math.random() * 50 + 20));
        }
        
        data.put("dates", dates);
        data.put("values", values);
        return data;
    }
    
    /**
     * 获取反馈类型分布数据
     */
    private Map<String, Object> getFeedbackTypeData() {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> series = new ArrayList<>();
        
        // 查询各类型的反馈数量
        long likeCount = feedbackMapper.count(null, null, "LIKE");
        long dislikeCount = feedbackMapper.count(null, null, "DISLIKE");
        long commentCount = feedbackMapper.count(null, null, "COMMENT");
        
        if (likeCount > 0) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "点赞");
            item.put("value", likeCount);
            series.add(item);
        }
        if (dislikeCount > 0) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "差评");
            item.put("value", dislikeCount);
            series.add(item);
        }
        if (commentCount > 0) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "评论");
            item.put("value", commentCount);
            series.add(item);
        }
        
        data.put("series", series);
        return data;
    }
}

