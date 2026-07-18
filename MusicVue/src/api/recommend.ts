import request from './request'
import type { RecommendRecord, AnalysisResult } from '@/types/music'

export function analyzeAudio(uploadId: number) {
  return request.post<string>(`/user/analyze/${uploadId}`)
}

interface RecommendationsResponse {
  uploadId: number
  recommendations: RecommendRecord[]
  count: number
}

export function getRecommendations(uploadId: number) {
  return request.get<RecommendationsResponse>(`/user/recommend/${uploadId}`)
}

