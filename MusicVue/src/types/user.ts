export interface User {
  id: number
  username: string
  phone?: string
  email?: string
  avatar?: string
  nickname?: string
  role: 'USER' | 'ADMIN'
  status: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  phone?: string
  email?: string
  emailCode?: string  // 邮箱验证码
  nickname?: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  role: string
  nickname?: string
  avatar?: string
}

