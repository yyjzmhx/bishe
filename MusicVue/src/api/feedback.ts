import request from './request'
import type { Music } from '@/types/music'

export type FeedbackType = 'LIKE' | 'DISLIKE' | 'COMMENT' | 'SUGGESTION' | 'FAVORITE'

export interface Feedback {
  id: number
  userId: number
  recommendId?: number
  musicId?: number
  type: FeedbackType
  content?: string
  createTime: string
}

export interface CommentVO {
  id: number
  userId: number
  musicId: number
  parentId?: number
  content: string
  createTime: string
  username: string
  nickname?: string
  avatar?: string
  likeCount?: number
  isLiked?: boolean
  replyCount?: number
  replies?: CommentVO[]
}

export interface FavoriteMusicVO extends Music {
  favoriteId: number
  favoriteTime: string
}

export interface CommunityPostVO extends CommentVO {
  musicTitle?: string
  musicArtist?: string
  musicCoverUrl?: string
}

export interface FeedbackListResponse {
  list: Feedback[]
  total?: number
  pageNum: number
  pageSize: number
}

export interface CommentListResponse {
  list: CommentVO[]
  total: number
  pageNum: number
  pageSize: number
}

export interface FavoriteListResponse {
  list: FavoriteMusicVO[]
  total: number
  pageNum: number
  pageSize: number
}

export interface CommunityListResponse {
  list: CommunityPostVO[]
  total: number
  pageNum: number
  pageSize: number
  sortType: 'latest' | 'hot'
}

export function submitFeedback(data: {
  recommendId?: number
  musicId?: number
  type: FeedbackType
  content?: string
}) {
  return request.post<string>('/user/feedback', data)
}

export function getFeedbackList(params?: {
  type?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<FeedbackListResponse>('/user/feedback/list', { params })
}

export function getFavoriteList(params?: {
  keyword?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<FavoriteListResponse>('/user/favorites', { params })
}

export function getCommunityFeed(params?: {
  keyword?: string
  pageNum?: number
  pageSize?: number
  sortType?: 'latest' | 'hot'
}) {
  return request.get<CommunityListResponse>('/user/community', { params })
}

export function getCommentsByMusicId(musicId: number, params?: {
  pageNum?: number
  pageSize?: number
  sortType?: 'time' | 'hot'
}) {
  return request.get<CommentListResponse>(`/user/music/${musicId}/comments`, { params })
}

export function submitComment(musicId: number, content: string) {
  return request.post<string>(`/user/music/${musicId}/comment`, { content })
}

export function toggleLike(musicId: number) {
  return request.post<{ isLiked: boolean }>(`/user/music/${musicId}/like`)
}

export function checkLikeStatus(musicId: number) {
  return request.get<{ isLiked: boolean }>(`/user/music/${musicId}/like/status`)
}

export function toggleFavorite(musicId: number) {
  return request.post<{ isFavorite: boolean }>(`/user/music/${musicId}/favorite`)
}

export function checkFavoriteStatus(musicId: number) {
  return request.get<{ isFavorite: boolean }>(`/user/music/${musicId}/favorite/status`)
}

export function deleteComment(commentId: number) {
  return request.delete<string>(`/user/comment/${commentId}`)
}

export function toggleCommentLike(commentId: number) {
  return request.post<{ isLiked: boolean; success: boolean }>(`/user/comment/${commentId}/like`)
}

export function getCommentLikeStatus(commentId: number) {
  return request.get<{ isLiked: boolean }>(`/user/comment/${commentId}/like/status`)
}

export function replyComment(commentId: number, content: string) {
  return request.post<string>(`/user/comment/${commentId}/reply`, { content })
}

export function getCommentReplies(commentId: number) {
  return request.get<CommentVO[]>(`/user/comment/${commentId}/replies`)
}
