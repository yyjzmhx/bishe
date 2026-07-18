import request from './request'
import type { Music } from '@/types/music'

interface MusicListResponse {
  list: Music[]
  total: number
  pageNum: number
  pageSize: number
}

export function getMusicList(params?: {
  keyword?: string
  artist?: string
  genre?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<MusicListResponse>('/user/music/list', { params })
}

export function getMusicDetail(id: number) {
  return request.get<Music>(`/user/music/${id}`)
}

export function getPreviewUrl(id: number) {
  return request.get<{ previewUrl: string }>(`/user/music/preview/${id}`)
}

