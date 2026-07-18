export interface Music {
  id: number
  title: string
  artist: string
  album?: string
  fileUrl: string
  previewUrl?: string
  duration?: number
  genre?: string
  lyrics?: string  // 歌词内容(LRC格式)
  coverUrl?: string
  playCount?: number
  likeCount?: number
  status?: number
}

export interface RecommendRecord {
  id: number
  uploadId: number
  musicId: number
  similarity: number
  rank: number
  music?: Music
  createTime?: string
}

export interface UploadRecord {
  id: number
  userId: number
  fileUrl: string
  fileName: string
  fileSize: number
  fileType?: string
  status: 'UPLOADING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'
  features?: string
  analysisResult?: string
  errorMessage?: string
  createTime?: string
}

export interface AnalysisResult {
  uploadId: number
  features: number[]
  analysisResult: string
  recommendations: RecommendRecord[]
}

