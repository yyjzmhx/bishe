/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

/**
 * 格式化时长（秒转分:秒）
 */
export function formatDuration(seconds: number | undefined | null): string {
  if (seconds === undefined || seconds === null || seconds < 0 || !isFinite(seconds) || isNaN(seconds)) {
    return '00:00'
  }
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

/**
 * 格式化日期
 */
export function formatDate(date: string | Date, format: string = 'YYYY-MM-DD HH:mm'): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  
  if (format === 'YYYY-MM-DD') {
    return `${year}-${month}-${day}`
  }
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

/**
 * 格式化相似度百分比
 */
export function formatSimilarity(similarity: number): string {
  return `${(similarity * 100).toFixed(1)}%`
}

