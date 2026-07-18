const TOKEN_KEY = 'music_token'
const ROLE_KEY = 'music_role'
const USER_INFO_KEY = 'music_user_info'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ROLE_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

export function setRole(role: string): void {
  localStorage.setItem(ROLE_KEY, role)
}

export function getRole(): string | null {
  return localStorage.getItem(ROLE_KEY)
}

export function setUserInfo(info: any): void {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
}

export function getUserInfo(): any | null {
  const info = localStorage.getItem(USER_INFO_KEY)
  return info ? JSON.parse(info) : null
}

