/**
 * 常量定义
 */

// API基础URL
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// 文件上传限制
export const MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
export const ALLOWED_FILE_TYPES = ['mp3', 'wav', 'flac', 'm4a', 'aac', 'ogg', 'mgg']

// 分页默认值
export const DEFAULT_PAGE_SIZE = 10
export const PAGE_SIZES = [10, 20, 50, 100]

// Token存储键
export const TOKEN_KEY = 'music_token'
export const ROLE_KEY = 'music_role'
export const USER_INFO_KEY = 'music_user_info'

