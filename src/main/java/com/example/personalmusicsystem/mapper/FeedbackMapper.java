package com.example.personalmusicsystem.mapper;

import com.example.personalmusicsystem.dto.response.CommentVO;
import com.example.personalmusicsystem.dto.response.CommunityPostVO;
import com.example.personalmusicsystem.dto.response.FavoriteMusicVO;
import com.example.personalmusicsystem.dto.response.FeedbackVO;
import com.example.personalmusicsystem.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 反馈Mapper接口
 */
@Mapper
public interface FeedbackMapper {
    
    /**
     * 插入反馈
     */
    int insert(Feedback feedback);
    
    /**
     * 查询反馈列表
     */
    List<Feedback> selectList(@Param("userId") Long userId,
                             @Param("musicId") Long musicId,
                             @Param("type") String type,
                             @Param("offset") Integer offset,
                             @Param("limit") Integer limit);
    
    /**
     * 根据音乐ID查询评论列表（带用户信息）
     */
    List<CommentVO> selectCommentsByMusicId(@Param("musicId") Long musicId,
                                          @Param("currentUserId") Long currentUserId,
                                          @Param("offset") Integer offset,
                                          @Param("limit") Integer limit);
    
    /**
     * 根据音乐ID查询评论列表（按热度排序）
     */
    List<CommentVO> selectCommentsByMusicIdOrderByHot(@Param("musicId") Long musicId,
                                                     @Param("currentUserId") Long currentUserId,
                                                     @Param("offset") Integer offset,
                                                     @Param("limit") Integer limit);

    /**
     * 查询收藏列表
     */
    List<FavoriteMusicVO> selectFavoriteMusicList(@Param("userId") Long userId,
                                                  @Param("keyword") String keyword,
                                                  @Param("offset") Integer offset,
                                                  @Param("limit") Integer limit);

    /**
     * 统计收藏列表数量
     */
    long countFavoriteMusicList(@Param("userId") Long userId,
                                @Param("keyword") String keyword);

    /**
     * 查询社区动态
     */
    List<CommunityPostVO> selectCommunityFeedByLatest(@Param("currentUserId") Long currentUserId,
                                                      @Param("keyword") String keyword,
                                                      @Param("offset") Integer offset,
                                                      @Param("limit") Integer limit);

    /**
     * 查询社区热门动态
     */
    List<CommunityPostVO> selectCommunityFeedByHot(@Param("currentUserId") Long currentUserId,
                                                   @Param("keyword") String keyword,
                                                   @Param("offset") Integer offset,
                                                   @Param("limit") Integer limit);

    /**
     * 统计社区动态数量
     */
    long countCommunityFeed(@Param("keyword") String keyword);
    
    /**
     * 检查用户是否已点赞评论
     */
    long checkCommentLike(@Param("userId") Long userId, @Param("commentId") Long commentId);
    
    /**
     * 添加评论点赞
     */
    int insertCommentLike(@Param("commentId") Long commentId, @Param("userId") Long userId);
    
    /**
     * 删除评论点赞
     */
    int deleteCommentLike(@Param("commentId") Long commentId, @Param("userId") Long userId);
    
    /**
     * 查询评论的回复列表
     */
    List<CommentVO> selectRepliesByCommentId(@Param("commentId") Long commentId,
                                            @Param("currentUserId") Long currentUserId,
                                            @Param("limit") Integer limit);
    
    /**
     * 删除反馈
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 统计反馈数量
     */
    long count(@Param("userId") Long userId,
               @Param("musicId") Long musicId,
               @Param("type") String type);
    
    /**
     * 统计音乐评论数量
     */
    long countCommentsByMusicId(@Param("musicId") Long musicId);
    
    /**
     * 检查用户是否已点赞
     */
    long checkUserLike(@Param("userId") Long userId, @Param("musicId") Long musicId);

    /**
     * 检查用户是否已收藏
     */
    long checkUserFavorite(@Param("userId") Long userId, @Param("musicId") Long musicId);

    /**
     * 根据用户、音乐和类型查询反馈
     */
    Feedback selectByUserAndMusicAndType(@Param("userId") Long userId,
                                         @Param("musicId") Long musicId,
                                         @Param("type") String type);
    
    /**
     * 根据ID查询反馈详情
     */
    Feedback selectById(@Param("id") Long id);
    
    /**
     * 管理员查询反馈列表（支持搜索、筛选、分页）
     */
    List<FeedbackVO> selectAdminList(@Param("keyword") String keyword,
                                    @Param("type") String type,
                                    @Param("startDate") String startDate,
                                    @Param("endDate") String endDate,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);
    
    /**
     * 管理员统计反馈数量
     */
    long countAdminList(@Param("keyword") String keyword,
                       @Param("type") String type,
                       @Param("startDate") String startDate,
                       @Param("endDate") String endDate);

    /**
     * 统计今日指定类型数量
     */
    long countTodayByType(@Param("type") String type);

    /**
     * 统计参与互动的用户数
     */
    long countDistinctUsersByTypes(@Param("types") String types);
    
    /**
     * 更新反馈（用于审核标记）
     */
    int update(Feedback feedback);
}

