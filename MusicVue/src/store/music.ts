import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { Music, RecommendRecord } from '@/types/music'
import { savePlaylist, getPlaylist, savePlayState, getPlayState, clearPlaylist } from '@/api/playlist'
import { useUserStore } from './user'

export type PlayMode = 'order' | 'singleLoop' | 'random'

export const useMusicStore = defineStore('music', () => {
  const currentMusic = ref<Music | null>(null)
  const isPlaying = ref(false)
  const recommendations = ref<RecommendRecord[]>([])
  const currentTime = ref(0)
  const duration = ref(0)
  
  // 播放列表相关
  const playlist = ref<Music[]>([])
  const currentIndex = ref(-1)
  const playMode = ref<PlayMode>('order')
  
  // 是否正在同步到服务器
  const isSyncing = ref(false)
  // 是否正在从服务器加载（加载时不触发同步）
  const isLoading = ref(false)
  // 是否已经加载过播放列表（避免重复加载）
  const hasLoaded = ref(false)
  
  // 计算属性：播放列表是否为空
  const isPlaylistEmpty = computed(() => playlist.value.length === 0)
  
  const userStore = useUserStore()
  
  function setCurrentMusic(music: Music | null) {
    currentMusic.value = music
  }
  
  function setPlaying(playing: boolean) {
    isPlaying.value = playing
  }
  
  function setRecommendations(recs: RecommendRecord[]) {
    recommendations.value = recs
  }
  
  function setCurrentTime(time: number) {
    currentTime.value = time
  }
  
  function setDuration(time: number) {
    duration.value = time
  }
  
  // 播放列表相关方法
  function setPlaylist(list: Music[], skipSync = false) {
    playlist.value = list
    if (list.length > 0 && currentIndex.value < 0) {
      currentIndex.value = 0
    }
    if (!skipSync) {
      syncPlaylist()
    }
  }
  
  function addToPlaylist(music: Music | Music[]) {
    if (Array.isArray(music)) {
      music.forEach(m => {
        const exists = playlist.value.some(item => item.id === m.id)
        if (!exists) {
          playlist.value.push(m)
        }
      })
    } else {
      // 检查是否已存在（根据ID判断）
      const exists = playlist.value.some(m => m.id === music.id)
      if (!exists) {
        playlist.value.push(music)
      }
    }
    syncPlaylist()
  }
  
  /**
   * 添加到播放列表的指定位置
   */
  function addToPlaylistAt(music: Music, index?: number) {
    const exists = playlist.value.some(m => m.id === music.id)
    if (exists) {
      return false
    }
    
    if (index !== undefined && index >= 0 && index < playlist.value.length) {
      playlist.value.splice(index, 0, music)
      // 如果插入位置在当前播放位置之前或等于，需要调整 currentIndex
      if (index <= currentIndex.value) {
        currentIndex.value++
      }
    } else {
      playlist.value.push(music)
    }
    
    syncPlaylist()
    return true
  }
  
  /**
   * 添加到下一首播放
   */
  function addToPlaylistNext(music: Music) {
    const exists = playlist.value.some(m => m.id === music.id)
    if (exists) {
      return false
    }
    
    const nextIndex = currentIndex.value + 1
    if (nextIndex < playlist.value.length && currentIndex.value >= 0) {
      playlist.value.splice(nextIndex, 0, music)
    } else {
      // 如果当前没有播放或已经是最后一首，直接添加到末尾
      playlist.value.push(music)
    }
    
    syncPlaylist()
    return true
  }
  
  function removeFromPlaylist(index: number) {
    if (index < 0 || index >= playlist.value.length) return
    
    const isCurrentItem = index === currentIndex.value
    const wasPlaying = isPlaying.value
    
    // 先删除项
    playlist.value.splice(index, 1)
    
    // 如果删除后列表为空
    if (playlist.value.length === 0) {
      currentIndex.value = -1
      currentMusic.value = null
      isPlaying.value = false
      syncPlaylist()
      syncPlayState()
      return
    }
    
    // 如果删除的是当前播放项
    if (isCurrentItem) {
      // 尝试播放下一首
      const nextMusic = playNext()
      if (nextMusic) {
        // playNext 已经更新了 currentIndex 和 currentMusic
        isPlaying.value = wasPlaying // 保持之前的播放状态
      } else {
        // 没有下一首（例如顺序播放到最后一首），尝试播放上一首
        const prevMusic = playPrevious()
        if (prevMusic) {
          isPlaying.value = wasPlaying
        } else {
          // 没有上一首也没有下一首，清空状态
          currentIndex.value = -1
          currentMusic.value = null
          isPlaying.value = false
        }
      }
    } else if (index < currentIndex.value) {
      // 如果删除的是当前播放项之前的项，需要调整索引
      currentIndex.value--
    }
    // 如果删除的是当前播放项之后的项，不需要调整索引
    
    syncPlaylist()
    syncPlayState()
  }
  
  function clearPlaylist() {
    playlist.value = []
    currentIndex.value = -1
    currentMusic.value = null
    isPlaying.value = false
    syncPlaylist()
    syncPlayState()
  }
  
  function setCurrentIndex(index: number) {
    if (index >= 0 && index < playlist.value.length) {
      currentIndex.value = index
      currentMusic.value = playlist.value[index]
      syncPlayState()
    }
  }
  
  function togglePlayMode() {
    const modes: PlayMode[] = ['order', 'singleLoop', 'random']
    const currentIndex = modes.indexOf(playMode.value)
    playMode.value = modes[(currentIndex + 1) % modes.length]
    syncPlayState()
  }
  
  function setPlayMode(mode: PlayMode) {
    playMode.value = mode
    syncPlayState()
  }
  
  function setCurrentMusicAndIndex(music: Music) {
    // 在播放列表中查找音乐
    const index = playlist.value.findIndex(m => m.id === music.id)
    if (index >= 0) {
      currentIndex.value = index
      currentMusic.value = music
    } else {
      // 如果不在列表中，添加到列表并设置
      addToPlaylist(music)
      currentIndex.value = playlist.value.length - 1
      currentMusic.value = music
    }
    syncPlayState()
  }
  
  // 同步播放列表到服务器
  async function syncPlaylist() {
    if (!userStore.isLoggedIn || isSyncing.value || isLoading.value) return
    
    try {
      isSyncing.value = true
      const musicIds = playlist.value.map(m => m.id!)
      await savePlaylist(musicIds)
    } catch (error) {
      console.error('同步播放列表失败:', error)
    } finally {
      isSyncing.value = false
    }
  }
  
  // 同步播放状态到服务器
  async function syncPlayState() {
    if (!userStore.isLoggedIn || isSyncing.value || isLoading.value) return
    
    try {
      isSyncing.value = true
      await savePlayState({
        currentMusicId: currentMusic.value?.id || null,
        currentIndex: currentIndex.value,
        playMode: playMode.value,
        currentTime: currentTime.value,
        isPlaying: isPlaying.value
      })
    } catch (error) {
      console.error('同步播放状态失败:', error)
    } finally {
      isSyncing.value = false
    }
  }
  
  // 从服务器加载播放列表和播放状态
  async function loadPlaylistFromServer() {
    if (!userStore.isLoggedIn || isLoading.value) return
    
    // 如果已经加载过，跳过（避免重复加载）
    if (hasLoaded.value) return
    
    try {
      isLoading.value = true
      
      // 加载播放列表
      const playlistResponse = await getPlaylist()
      console.log('加载播放列表响应:', playlistResponse)
      if (playlistResponse && Array.isArray(playlistResponse)) {
        setPlaylist(playlistResponse, true) // 跳过同步，避免循环
      }
      
      // 加载播放状态
      const stateResponse = await getPlayState()
      console.log('加载播放状态响应:', stateResponse)
      if (stateResponse) {
        if (stateResponse.currentMusic) {
          currentMusic.value = stateResponse.currentMusic
        }
        if (stateResponse.currentIndex !== null && stateResponse.currentIndex !== undefined) {
          currentIndex.value = stateResponse.currentIndex
        }
        if (stateResponse.playMode) {
          playMode.value = stateResponse.playMode as PlayMode
        }
        if (stateResponse.currentTime !== null && stateResponse.currentTime !== undefined) {
          currentTime.value = stateResponse.currentTime
        }
        if (stateResponse.isPlaying !== null && stateResponse.isPlaying !== undefined) {
          isPlaying.value = stateResponse.isPlaying
        }
      }
      
      hasLoaded.value = true
    } catch (error) {
      console.error('加载播放列表失败:', error)
    } finally {
      isLoading.value = false
    }
  }
  
  // 重置加载状态（退出登录时调用）
  function resetLoadState() {
    hasLoaded.value = false
  }
  
  // 清除服务器上的播放数据
  async function clearServerPlaylist() {
    if (!userStore.isLoggedIn) return
    
    try {
      await clearPlaylist()
    } catch (error) {
      console.error('清除播放数据失败:', error)
    }
  }
  
  function playNext(): Music | null {
    if (playlist.value.length === 0 || currentIndex.value < 0) return null
    
    // 单曲循环模式：直接返回当前歌曲
    if (playMode.value === 'singleLoop') {
      return currentMusic.value
    }
    
    let nextIndex = -1
    
    switch (playMode.value) {
      case 'order':
        // 顺序播放：下一首，如果是最后一首则循环到第一首
        if (currentIndex.value < playlist.value.length - 1) {
          nextIndex = currentIndex.value + 1
        } else {
          // 最后一首，循环到第一首
          nextIndex = 0
        }
        break
        
      case 'random':
        // 随机播放：随机选择一首（避免连续播放同一首）
        if (playlist.value.length === 1) {
          nextIndex = 0
        } else {
          let randomIndex
          do {
            randomIndex = Math.floor(Math.random() * playlist.value.length)
          } while (randomIndex === currentIndex.value && playlist.value.length > 1)
          nextIndex = randomIndex
        }
        break
    }
    
    if (nextIndex >= 0 && nextIndex < playlist.value.length) {
      currentIndex.value = nextIndex
      currentMusic.value = playlist.value[nextIndex]
      return currentMusic.value
    }
    
    return null
  }
  
  function playPrevious(): Music | null {
    if (playlist.value.length === 0 || currentIndex.value < 0) return null
    
    // 单曲循环模式：直接返回当前歌曲
    if (playMode.value === 'singleLoop') {
      return currentMusic.value
    }
    
    let prevIndex = -1
    
    switch (playMode.value) {
      case 'order':
        // 顺序播放：上一首，如果是第一首则跳到最后一首
        if (currentIndex.value > 0) {
          prevIndex = currentIndex.value - 1
        } else {
          prevIndex = playlist.value.length - 1
        }
        break
        
      case 'random':
        // 随机播放：随机选择一首
        if (playlist.value.length === 1) {
          prevIndex = 0
        } else {
          let randomIndex
          do {
            randomIndex = Math.floor(Math.random() * playlist.value.length)
          } while (randomIndex === currentIndex.value && playlist.value.length > 1)
          prevIndex = randomIndex
        }
        break
    }
    
    if (prevIndex >= 0 && prevIndex < playlist.value.length) {
      currentIndex.value = prevIndex
      currentMusic.value = playlist.value[prevIndex]
      return currentMusic.value
    }
    
    return null
  }
  
  // 监听播放状态变化，自动同步（使用防抖，避免频繁请求）
  let syncTimer: ReturnType<typeof setTimeout> | null = null
  watch([currentMusic, isPlaying, currentTime], () => {
    if (syncTimer) {
      clearTimeout(syncTimer)
    }
    syncTimer = setTimeout(() => {
      syncPlayState()
    }, 1000) // 1秒防抖
  }, { deep: true })
  
  return {
    currentMusic,
    isPlaying,
    recommendations,
    currentTime,
    duration,
    playlist,
    currentIndex,
    playMode,
    isPlaylistEmpty,
    setCurrentMusic,
    setPlaying,
    setRecommendations,
    setCurrentTime,
    setDuration,
    setPlaylist,
    addToPlaylist,
    addToPlaylistAt,
    addToPlaylistNext,
    removeFromPlaylist,
    clearPlaylist,
    setCurrentIndex,
    togglePlayMode,
    setPlayMode,
    setCurrentMusicAndIndex,
    playNext,
    playPrevious,
    syncPlaylist,
    syncPlayState,
    loadPlaylistFromServer,
    clearServerPlaylist,
    resetLoadState
  }
})

