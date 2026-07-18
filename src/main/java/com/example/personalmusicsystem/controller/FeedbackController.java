package com.example.personalmusicsystem.controller;

import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.dto.response.CommentVO;
import com.example.personalmusicsystem.dto.response.CommunityPostVO;
import com.example.personalmusicsystem.dto.response.FavoriteMusicVO;
import com.example.personalmusicsystem.entity.Feedback;
import com.example.personalmusicsystem.service.FeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 反馈控制器
 */
@RestController
@RequestMapping("/api/user")
@Slf4j
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    /**
     * 提交反馈
     */
    @PostMapping("/feedback")
    public Result<String> submitFeedback(@RequestBody Feedback feedback,
                                         @RequestAttribute Long userId) {
        feedback.setUserId(userId);

        boolean success = feedbackService.submitFeedback(feedback);
        if (success) {
            return Result.success("反馈提交成功", null);
        }
        return Result.error("反馈提交失败");
    }

    /**
     * 获取反馈列表
     */
    @GetMapping("/feedback/list")
    public Result<Map<String, Object>> getFeedbackList(@RequestAttribute Long userId,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        List<Feedback> list = feedbackService.getFeedbackList(userId, null, type, pageNum, pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 获取收藏列表
     */
    @GetMapping("/favorites")
    public Result<Map<String, Object>> getFavoriteList(@RequestAttribute Long userId,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "12") Integer pageSize) {
        List<FavoriteMusicVO> list = feedbackService.getFavoriteMusicList(userId, keyword, pageNum, pageSize);
        long total = feedbackService.countFavoriteMusicList(userId, keyword);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    /**
     * 获取社区动态
     */
    @GetMapping("/community")
    public Result<Map<String, Object>> getCommunityFeed(@RequestAttribute Long userId,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        @RequestParam(defaultValue = "latest") String sortType) {
        List<CommunityPostVO> list = feedbackService.getCommunityFeed(userId, keyword, pageNum, pageSize, sortType);
        long total = feedbackService.countCommunityFeed(keyword);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("sortType", sortType);
        return Result.success(result);
    }

    /**
     * 获取歌曲评论列表
     */
    @GetMapping("/music/{musicId}/comments")
    public Result<Map<String, Object>> getCommentsByMusicId(@PathVariable Long musicId,
                                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                                            @RequestParam(defaultValue = "time") String sortType,
                                                            @RequestAttribute(required = false) Long userId) {
        List<CommentVO> list = feedbackService.getCommentsByMusicId(musicId, userId, pageNum, pageSize, sortType);
        long total = feedbackService.countCommentsByMusicId(musicId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 提交评论
     */
    @PostMapping("/music/{musicId}/comment")
    public Result<String> submitComment(@PathVariable Long musicId,
                                        @RequestBody Map<String, String> request,
                                        @RequestAttribute Long userId) {
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }

        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setMusicId(musicId);
        feedback.setType("COMMENT");
        feedback.setContent(content.trim());

        boolean success = feedbackService.submitFeedback(feedback);
        if (success) {
            return Result.success("评论成功", null);
        }
        return Result.error("评论失败");
    }

    /**
     * 点赞/取消点赞歌曲
     */
    @PostMapping("/music/{musicId}/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long musicId,
                                                  @RequestAttribute Long userId) {
        boolean isLiked = feedbackService.checkUserLike(userId, musicId);

        if (isLiked) {
            List<Feedback> likes = feedbackService.getFeedbackList(userId, musicId, "LIKE", 1, 1);
            if (!likes.isEmpty()) {
                feedbackService.deleteFeedback(likes.get(0).getId());
            }
        } else {
            Feedback feedback = new Feedback();
            feedback.setUserId(userId);
            feedback.setMusicId(musicId);
            feedback.setType("LIKE");
            feedbackService.submitFeedback(feedback);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", !isLiked);
        return Result.success(result);
    }

    /**
     * 获取歌曲点赞状态
     */
    @GetMapping("/music/{musicId}/like/status")
    public Result<Map<String, Boolean>> checkLikeStatus(@PathVariable Long musicId,
                                                        @RequestAttribute Long userId) {
        boolean isLiked = feedbackService.checkUserLike(userId, musicId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("isLiked", isLiked);
        return Result.success(result);
    }

    /**
     * 收藏/取消收藏歌曲
     */
    @PostMapping("/music/{musicId}/favorite")
    public Result<Map<String, Object>> toggleFavorite(@PathVariable Long musicId,
                                                      @RequestAttribute Long userId) {
        boolean isFavorite = feedbackService.toggleFavorite(musicId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("isFavorite", isFavorite);
        return Result.success(isFavorite ? "收藏成功" : "已取消收藏", result);
    }

    /**
     * 获取歌曲收藏状态
     */
    @GetMapping("/music/{musicId}/favorite/status")
    public Result<Map<String, Boolean>> checkFavoriteStatus(@PathVariable Long musicId,
                                                            @RequestAttribute Long userId) {
        boolean isFavorite = feedbackService.checkUserFavorite(userId, musicId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("isFavorite", isFavorite);
        return Result.success(result);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comment/{commentId}")
    public Result<String> deleteComment(@PathVariable Long commentId,
                                        @RequestAttribute Long userId) {
        Feedback feedback = feedbackService.getFeedbackById(commentId);
        if (feedback == null) {
            return Result.error("评论不存在");
        }
        if (!feedback.getUserId().equals(userId)) {
            return Result.error("无权删除该评论");
        }

        boolean success = feedbackService.deleteFeedback(commentId);
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }

    /**
     * 点赞/取消点赞评论
     */
    @PostMapping("/comment/{commentId}/like")
    public Result<Map<String, Object>> toggleCommentLike(@PathVariable Long commentId,
                                                         @RequestAttribute Long userId) {
        try {
            boolean success = feedbackService.toggleCommentLike(commentId, userId);
            boolean isLiked = feedbackService.checkCommentLike(commentId, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("isLiked", isLiked);
            result.put("success", success);
            return Result.success(isLiked ? "点赞成功" : "已取消点赞", result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Comment like toggle failed", e);
            return Result.error("操作失败");
        }
    }

    /**
     * 回复评论
     */
    @PostMapping("/comment/{commentId}/reply")
    public Result<String> replyComment(@PathVariable Long commentId,
                                       @RequestBody Map<String, String> request,
                                       @RequestAttribute Long userId) {
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return Result.error("回复内容不能为空");
        }

        Feedback parentComment = feedbackService.getFeedbackById(commentId);
        if (parentComment == null || !"COMMENT".equals(parentComment.getType())) {
            return Result.error("评论不存在");
        }

        Feedback reply = new Feedback();
        reply.setUserId(userId);
        reply.setMusicId(parentComment.getMusicId());
        reply.setParentId(commentId);
        reply.setType("COMMENT");
        reply.setContent(content.trim());

        boolean success = feedbackService.submitFeedback(reply);
        if (success) {
            return Result.success("回复成功", null);
        }
        return Result.error("回复失败");
    }

    /**
     * 获取评论点赞状态
     */
    @GetMapping("/comment/{commentId}/like/status")
    public Result<Map<String, Object>> getCommentLikeStatus(@PathVariable Long commentId,
                                                            @RequestAttribute Long userId) {
        boolean isLiked = feedbackService.checkCommentLike(commentId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        return Result.success(result);
    }

    /**
     * 获取评论回复列表
     */
    @GetMapping("/comment/{commentId}/replies")
    public Result<List<CommentVO>> getCommentReplies(@PathVariable Long commentId,
                                                     @RequestAttribute(required = false) Long userId) {
        List<CommentVO> replies = feedbackService.getRepliesByCommentId(commentId, userId);
        return Result.success(replies);
    }
}
