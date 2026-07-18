import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/user'
import { getToken, setToken, removeToken, setRole, setUserInfo as setUserInfoStorage, getUserInfo as getUserInfoStorage, getRole } from '@/utils/auth'
import { useMusicStore } from './music'
import { clearPlaylist } from '@/api/playlist'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  // 初始化时从 localStorage 读取用户信息
  const userInfo = ref<User | null>(getUserInfoStorage())
  
  const isLoggedIn = computed(() => !!token.value)
  // 优先使用内存中的 userInfo，如果没有则从 localStorage 读取
  const isAdmin = computed(() => {
    if (userInfo.value?.role === 'ADMIN') return true
    const storedRole = getRole()
    return storedRole === 'ADMIN'
  })
  
  function setUserInfo(info: User) {
    userInfo.value = info
    setUserInfoStorage(info)
  }
  
  function setUserToken(t: string, role?: string) {
    token.value = t
    setToken(t)
    if (role) {
      setRole(role)
    }
  }
  
  async function logout() {
    // 清除服务器上的播放列表数据
    try {
      await clearPlaylist()
    } catch (error) {
      console.error('清除播放列表失败:', error)
    }
    
    // 清除本地音乐状态
    const musicStore = useMusicStore()
    musicStore.clearPlaylist()
    musicStore.clearServerPlaylist()
    musicStore.resetLoadState() // 重置加载状态
    
    token.value = ''
    userInfo.value = null
    removeToken()
  }
  
  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    setUserInfo,
    setUserToken,
    logout
  }
})

