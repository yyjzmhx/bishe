package com.example.personalmusicsystem.service;

import com.example.personalmusicsystem.dto.response.CommentVO;
import com.example.personalmusicsystem.dto.response.CommunityPostVO;
import com.example.personalmusicsystem.dto.response.FavoriteMusicVO;
import com.example.personalmusicsystem.entity.Feedback;
import com.example.personalmusicsystem.mapper.FeedbackMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 反馈服务
 */
@Service
@Slf4j
public class FeedbackService {

    @Autowired
    private FeedbackMapper feedbackMapper;

    /**
     * 提交反馈
     */
    public boolean submitFeedback(Feedback feedback) {
        return feedbackMapper.insert(feedback) > 0;
    }

    /**
     * 查询反馈列表
     */
    public List<Feedback> getFeedbackList(Long userId, Long musicId, String type, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return feedbackMapper.selectList(userId, musicId, type, offset, pageSize);
    }

    /**
     * 获取收藏列表
     */
    public List<FavoriteMusicVO> getFavoriteMusicList(Long userId, String keyword, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return feedbackMapper.selectFavoriteMusicList(userId, keyword, offset, pageSize);
    }

    /**
     * 统计收藏数量
     */
    public long countFavoriteMusicList(Long userId, String keyword) {
        return feedbackMapper.countFavoriteMusicList(userId, keyword);
    }

    /**
     * 获取社区动态
     */
    public List<CommunityPostVO> getCommunityFeed(Long currentUserId, String keyword, int pageNum, int pageSize, String sortType) {
        int offset = (pageNum - 1) * pageSize;
        if ("hot".equalsIgnoreCase(sortType)) {
            return feedbackMapper.selectCommunityFeedByHot(currentUserId, keyword, offset, pageSize);
        }
        return feedbackMapper.selectCommunityFeedByLatest(currentUserId, keyword, offset, pageSize);
    }

    /**
     * 统计社区动态数量
     */
    public long countCommunityFeed(String keyword) {
        return feedbackMapper.countCommunityFeed(keyword);
    }

    /**
     * 根据音乐 ID 获取评论列表
     */
    public List<CommentVO> getCommentsByMusicId(Long musicId, Long currentUserId, int pageNum, int pageSize, String sortType) {
        int offset = (pageNum - 1) * pageSize;
        if ("hot".equalsIgnoreCase(sortType)) {
            return feedbackMapper.selectCommentsByMusicIdOrderByHot(musicId, currentUserId, offset, pageSize);
        }
        return feedbackMapper.selectCommentsByMusicId(musicId, currentUserId, offset, pageSize);
    }

    /**
     * 切换评论点赞状态
     */
    @Transactional
    public boolean toggleCommentLike(Long commentId, Long userId) {
        Feedback comment = feedbackMapper.selectById(commentId);
        if (comment == null || !"COMMENT".equals(comment.getType())) {
            throw new RuntimeException("评论不存在");
        }

        if (comment.getUserId().equals(userId)) {
            throw new RuntimeException("不能给自己的评论点赞");
        }

        boolean isLiked = feedbackMapper.checkCommentLike(userId, commentId) > 0;
        if (isLiked) {
            return feedbackMapper.deleteCommentLike(commentId, userId) > 0;
        }
        return feedbackMapper.insertCommentLike(commentId, userId) > 0;
    }

    /**
     * 切换收藏状态
     */
    @Transactional
    public boolean toggleFavorite(Long musicId, Long userId) {
        Feedback favorite = feedbackMapper.selectByUserAndMusicAndType(userId, musicId, "FAVORITE");
        if (favorite != null) {
            feedbackMapper.deleteById(favorite.getId());
            return false;
        }

        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setMusicId(musicId);
        feedback.setType("FAVORITE");
        feedbackMapper.insert(feedback);
        return true;
    }

    /**
     * 检查评论点赞状态
     */
    public boolean checkCommentLike(Long commentId, Long userId) {
        return feedbackMapper.checkCommentLike(userId, commentId) > 0;
    }

    /**
     * 统计评论数量
     */
    public long countCommentsByMusicId(Long musicId) {
        return feedbackMapper.countCommentsByMusicId(musicId);
    }

    /**
     * 检查歌曲点赞状态
     */
    public boolean checkUserLike(Long userId, Long musicId) {
        return feedbackMapper.checkUserLike(userId, musicId) > 0;
    }

    /**
     * 检查歌曲收藏状态
     */
    public boolean checkUserFavorite(Long userId, Long musicId) {
        return feedbackMapper.checkUserFavorite(userId, musicId) > 0;
    }

    /**
     * 删除反馈
     */
    public boolean deleteFeedback(Long id) {
        return feedbackMapper.deleteById(id) > 0;
    }

    /**
     * 根据 ID 获取反馈
     */
    public Feedback getFeedbackById(Long id) {
        return feedbackMapper.selectById(id);
    }

    /**
     * 获取评论回复列表
     */
    public List<CommentVO> getRepliesByCommentId(Long commentId, Long currentUserId) {
        return feedbackMapper.selectRepliesByCommentId(commentId, currentUserId, 50);
    }
}
