import request from './request'
import type { UploadRecord } from '@/types/music'

export function uploadFile(
  formData: FormData,
  onProgress?: (progress: number) => void
) {
  return request.post<{
    uploadId: number
    fileUrl: string
    fileName: string
    fileSize: number
    status: string
  }>('/user/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const progress = progressEvent.loaded / progressEvent.total
        onProgress(progress)
      }
    }
  })
}

export function getUploadRecord(id: number) {
  return request.get<UploadRecord>(`/user/upload/${id}`)
}

export function getUploadHistory(
  pageNum = 1,
  pageSize = 10,
  keyword?: string,
  startDate?: string,
  endDate?: string
) {
  const params: any = { pageNum, pageSize }
  if (keyword) params.keyword = keyword
  if (startDate) params.startDate = startDate
  if (endDate) params.endDate = endDate
  
  return request.get<{
    list: UploadRecord[]
    total: number
    pageNum: number
    pageSize: number
  }>('/user/upload/history', {
    params
  })
}

export function deleteUploadRecord(id: number) {
  return request.delete(`/user/upload/${id}`)
}

