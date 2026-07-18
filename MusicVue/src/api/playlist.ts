import request from './request'
import type { Music } from '@/types/music'

/**
 * 保存播放列表
 */
export function savePlaylist(musicIds: number[]) {
  return request.post<string>('/user/playlist/save', musicIds)
}

/**
 * 获取播放列表
 */
export function getPlaylist() {
  return request.get<Music[]>('/user/playlist/get')
}

/**
 * 保存播放状态
 */
export function savePlayState(data: {
  currentMusicId: number | null
  currentIndex: number
  playMode: string
  currentTime: number
  isPlaying: boolean
}) {
  return request.post<string>('/user/playlist/state/save', data)
}

/**
 * 获取播放状态
 */
export function getPlayState() {
  return request.get<{
    currentMusicId: number | null
    currentIndex: number | null
    playMode: string | null
    currentTime: number | null
    isPlaying: boolean | null
    currentMusic: Music | null
  }>('/user/playlist/state/get')
}

/**
 * 清除播放数据（退出登录时调用）
 */
export function clearPlaylist() {
  return request.post<string>('/user/playlist/clear')
}

