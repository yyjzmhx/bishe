import request from './request'
import type { LoginRequest, RegisterRequest, LoginResponse } from '@/types/user'

export function login(data: LoginRequest) {
  return request.post<LoginResponse>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<string>('/auth/register', data)
}

export function getCurrentUser() {
  return request.get('/auth/current')
}

export interface ForgotPasswordRequest {
  email: string
}

export interface VerifyCodeRequest {
  email: string
  code: string
}

export interface ResetPasswordRequest {
  email: string
  code: string
  password: string
}

export function forgotPassword(data: ForgotPasswordRequest) {
  return request.post<string>('/auth/forgot-password', data)
}

export function verifyCode(data: VerifyCodeRequest) {
  return request.post<{ email: string; code: string }>('/auth/verify-code', data)
}

export function resetPassword(data: ResetPasswordRequest) {
  return request.post<string>('/auth/reset-password', data)
}

export interface SendRegisterCodeRequest {
  email: string
}

export function sendRegisterCode(data: SendRegisterCodeRequest) {
  return request.post<string>('/auth/register/send-code', data)
}

