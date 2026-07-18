<template>
  <div class="audio-player-container" v-if="musicStore.currentMusic || musicStore.playlist.length > 0">
    <!-- 悬浮胶囊播放器主体 -->
    <div class="audio-player-capsule">
      <!-- 左侧：歌曲信息区 -->
      <div class="section-info">
        <div class="cover-box" @click="handleCoverClick">
          <img
            :src="musicStore.currentMusic?.coverUrl || defaultCover"
            :alt="musicStore.currentMusic?.title || 'Music'"
            class="music-cover"
          />
          <div class="cover-mask">
            <el-icon><Expand /></el-icon>
          </div>
        </div>
        <div class="meta-info">
          <div class="title-row">
            <span class="music-title" :title="musicStore.currentMusic?.title || '暂无播放'">
              {{ musicStore.currentMusic?.title || '暂无播放' }}
            </span>
            <span class="music-artist" :title="musicStore.currentMusic?.artist || ''">
              {{ musicStore.currentMusic?.artist || '' }}
            </span>
          </div>
          <div class="action-row">
            <el-icon class="action-icon" title="喜欢"><Star /></el-icon>
            <el-icon class="action-icon" title="不喜欢"><CircleClose /></el-icon>
            <el-icon class="action-icon" title="评论">
              <el-badge :value="125" class="comment-badge" type="info">
                <ChatDotRound />
              </el-badge>
            </el-icon>
            <el-icon class="action-icon" title="更多"><MoreFilled /></el-icon>
          </div>
        </div>
      </div>

      <!-- 中间：控制与进度区 -->
      <div class="section-control">
        <!-- 上半部分：控制按钮 -->
        <div class="control-buttons">
          <el-tooltip :content="playModeText" placement="top" :show-after="500">
            <el-icon class="ctrl-icon mode" @click="togglePlayMode" :class="{ 'active': musicStore.playMode !== 'order' }">
              <component :is="playModeIcon" />
            </el-icon>
          </el-tooltip>
          
          <el-icon class="ctrl-icon prev" @click="handlePrevious"><CaretLeft /></el-icon>
          
          <div class="play-btn-circle" @click="togglePlay">
            <el-icon class="play-icon">
              <component :is="musicStore.isPlaying ? VideoPause : VideoPlay" />
            </el-icon>
          </div>
          
          <el-icon class="ctrl-icon next" @click="handleNext"><CaretRight /></el-icon>
          
          <div class="volume-box">
            <el-icon class="ctrl-icon volume" @click="toggleMute">
              <component :is="isMuted || volume === 0 ? Mute : Microphone" />
            </el-icon>
            <!-- 音量滑块悬停显示 -->
            <div class="volume-slider-popup">
              <el-slider v-model="volume" vertical height="80px" @input="handleVolumeChange" />
            </div>
          </div>
        </div>
        
        <!-- 下半部分：进度条 -->
        <div class="progress-bar-row">
          <span class="time-label">{{ formatTime(currentTime) }}</span>
          <div class="slider-wrapper">
            <el-slider
              v-model="currentTime"
              :max="(musicStore.duration && musicStore.duration > 0) ? musicStore.duration : 100"
              :show-tooltip="false"
              @change="handleSeek"
              @input="handleSliderInput"
              class="custom-slider"
            />
          </div>
          <span class="time-label">{{ formatTime(musicStore.duration) }}</span>
        </div>
      </div>

      <!-- 右侧：功能区 -->
      <div class="section-features">
        <span class="tag-hq">HQ</span>
        <el-tooltip content="音效" placement="top">
          <el-icon class="feat-icon"><Headset /></el-icon>
        </el-tooltip>
        <el-tooltip content="歌词" placement="top">
          <div class="lyrics-btn-icon">词</div>
        </el-tooltip>
        <el-tooltip content="播放列表" placement="top">
          <el-icon class="feat-icon" @click="showPlaylist = true"><Menu /></el-icon>
        </el-tooltip>
      </div>
    </div>
    
    <!-- 播放列表抽屉 -->
    <PlaylistDrawer v-model="showPlaylist" />
    
    <audio
      ref="audioRef"
      :src="audioSrc"
      @timeupdate="handleTimeUpdate"
      @loadedmetadata="handleLoadedMetadata"
      @ended="handleEnded"
      @error="handleError"
      @loadstart="handleLoadStart"
      @canplay="handleCanPlay"
      @canplaythrough="handleCanPlayThrough"
      preload="metadata"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { 
  VideoPlay, VideoPause, CaretLeft, CaretRight, 
  Microphone, Mute, Star, ChatDotRound, 
  MoreFilled, Refresh, Menu, Setting, MagicStick,
  ArrowDown, Expand, CircleClose, Headset, Sort, Operation
} from '@element-plus/icons-vue'
import { useMusicStore } from '@/store/music'
import { formatDuration } from '@/utils/format'
import { ElMessage } from 'element-plus'
import PlaylistDrawer from './PlaylistDrawer.vue'

const musicStore = useMusicStore()
const router = useRouter()
const audioRef = ref<HTMLAudioElement>()
const currentTime = ref(0)
const volume = ref(100)
const isMuted = ref(false)
const defaultCover = 'https://via.placeholder.com/64x64?text=Music'
const audioLoadError = ref(false)
const isDragging = ref(false)
const showPlaylist = ref(false)

// 点击封面跳转到歌曲详情页
const handleCoverClick = () => {
  // 如果没有当前音乐但播放列表有歌曲，设置第一首为当前音乐
  if (!musicStore.currentMusic && musicStore.playlist.length > 0) {
    musicStore.setCurrentMusicAndIndex(musicStore.playlist[0])
  }
  if (musicStore.currentMusic?.id) {
    router.push({
      name: 'MusicDetail',
      params: { id: musicStore.currentMusic.id }
    })
  }
}

// 播放模式文本
const playModeText = computed(() => {
  switch (musicStore.playMode) {
    case 'order':
      return '顺序播放'
    case 'singleLoop':
      return '单曲循环'
    case 'random':
      return '随机播放'
    default:
      return '顺序播放'
  }
})

// 播放模式图标
const playModeIcon = computed(() => {
  switch (musicStore.playMode) {
    case 'order':
      return Sort
    case 'singleLoop':
      return Refresh
    case 'random':
      return Operation
    default:
      return Sort
  }
})

// 切换播放模式
const togglePlayMode = () => {
  musicStore.togglePlayMode()
  const modeNames: Record<string, string> = { singleLoop: '单曲循环',
    order: '顺序播放',
    loop: '单曲循环',
    random: '随机播放'
  }
  ElMessage.success(`已切换为${modeNames[musicStore.playMode]}`)
}

// 计算音频源URL
const audioSrc = computed(() => {
  if (!musicStore.currentMusic?.fileUrl) {
    console.warn('当前音乐没有fileUrl', musicStore.currentMusic)
    return ''
  }
  
  const fileUrl = musicStore.currentMusic.fileUrl
  console.debug('原始fileUrl:', fileUrl)
  
  // 如果已经是 /api/files/ 开头的路径，直接返回
  if (fileUrl.startsWith('/api/files/')) {
    console.debug('使用/api/files/路径:', fileUrl)
    return fileUrl
  }
  
  // 处理MinIO URL格式
  if (fileUrl.includes('localhost:9000') || fileUrl.includes('music-storage')) {
    try {
      const url = new URL(fileUrl)
      const pathParts = url.pathname.split('/').filter(p => p.length > 0)
      console.debug('解析URL路径部分:', pathParts)
      
      const bucketIndex = pathParts.indexOf('music-storage')
      if (bucketIndex !== -1 && bucketIndex < pathParts.length - 1) {
        const filePath = pathParts.slice(bucketIndex + 1).join('/')
        const result = `/api/files/${filePath}`
        console.debug('转换后的路径 (方式1):', result)
        return result
      }
      
      if (pathParts.length > 1 && pathParts[0] === 'music-storage') {
        const filePath = pathParts.slice(1).join('/')
        const result = `/api/files/${filePath}`
        console.debug('转换后的路径 (方式2):', result)
        return result
      }
      
      const match = fileUrl.match(/music-storage[\/\\](.+)$/)
      if (match) {
        const result = `/api/files/${match[1]}`
        console.debug('转换后的路径 (方式3):', result)
        return result
      }
      
      console.warn('无法解析MinIO URL，返回原始URL:', fileUrl)
      return fileUrl
    } catch (e) {
      console.error('URL解析失败:', e, '原始URL:', fileUrl)
      const match = fileUrl.match(/music-storage[\/\\](.+)$/)
      if (match) {
        const result = `/api/files/${match[1]}`
        console.debug('使用正则匹配转换:', result)
        return result
      }
      return fileUrl
    }
  }
  
  // 如果fileUrl是相对路径（如 library/music_2.mp3），直接添加 /api/files/ 前缀
  if (!fileUrl.startsWith('http://') && !fileUrl.startsWith('https://') && !fileUrl.startsWith('/')) {
    const result = `/api/files/${fileUrl}`
    console.debug('相对路径转换:', result)
    return result
  }
  
  console.debug('返回原始fileUrl:', fileUrl)
  return fileUrl
})

const formatTime = (seconds?: number | null) => {
  if (seconds === undefined || seconds === null || !isFinite(seconds) || isNaN(seconds)) {
    return '00:00'
  }
  return formatDuration(seconds)
}

const handleError = (event: Event) => {
  const audio = event.target as HTMLAudioElement
  audioLoadError.value = true
  if (!audio.src || audio.src === window.location.href) return
  
  const audioSrcValue = audioSrc.value
  console.error('音频加载失败', {
    error: audio.error,
    errorCode: audio.error?.code,
    src: audioSrcValue,
    fileUrl: musicStore.currentMusic?.fileUrl,
    musicId: musicStore.currentMusic?.id
  })
  
  if (audio.error) {
    switch (audio.error.code) {
      case MediaError.MEDIA_ERR_ABORTED:
        // 用户中止加载，不显示错误
        return
      case MediaError.MEDIA_ERR_NETWORK:
        console.error('网络错误，无法加载音频')
        ElMessage.error('网络错误，无法加载音频文件')
        break
      case MediaError.MEDIA_ERR_DECODE:
        console.error('音频解码失败')
        ElMessage.error('音频解码失败，文件可能已损坏')
        break
      case MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED:
        console.error('不支持的音频格式或源不可用')
        ElMessage.error('音频文件不存在或格式不支持')
        break
      default:
        console.error(`音频加载失败 (错误代码: ${audio.error.code})`)
        ElMessage.error(`音频加载失败 (错误代码: ${audio.error.code})`)
    }
    musicStore.setPlaying(false)
  }
}

const handleLoadStart = () => { audioLoadError.value = false }

const handleCanPlay = () => {
  if (musicStore.isPlaying && audioRef.value) {
    audioRef.value.play().catch(() => {})
  }
}

const handleCanPlayThrough = () => {}

const togglePlay = async () => {
  // 如果没有当前音乐但播放列表有歌曲，设置第一首为当前音乐
  if (!musicStore.currentMusic && musicStore.playlist.length > 0) {
    musicStore.setCurrentMusicAndIndex(musicStore.playlist[0])
    // 等待一下让音频元素加载
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  
  if (!audioRef.value) return
  if (!audioSrc.value) {
    ElMessage.warning('音频文件不存在')
    return
  }
  try {
    if (musicStore.isPlaying) {
      audioRef.value.pause()
      musicStore.setPlaying(false)
    } else {
      await audioRef.value.play()
      musicStore.setPlaying(true)
    }
  } catch (error: any) {
    if (error.name !== 'AbortError') musicStore.setPlaying(false)
  }
}

const handleSeek = (value: number) => {
  isDragging.value = false
  if (audioRef.value) {
    audioRef.value.currentTime = value
  }
  currentTime.value = value
  musicStore.setCurrentTime(value)
}

const handleSliderInput = (value: number) => {
  isDragging.value = true
  currentTime.value = value
  if (audioRef.value) {
    audioRef.value.currentTime = value
  }
  musicStore.setCurrentTime(value)
}

const handleTimeUpdate = () => {
  if (audioRef.value && !isDragging.value) {
    const time = audioRef.value.currentTime
    if (isFinite(time) && !isNaN(time)) {
      currentTime.value = time
      musicStore.setCurrentTime(time)
    }
  }
}

const handleLoadedMetadata = () => {
  if (audioRef.value && audioRef.value.duration && isFinite(audioRef.value.duration)) {
    musicStore.setDuration(audioRef.value.duration)
  }
}

const handleEnded = () => {
  musicStore.setPlaying(false)
  musicStore.setCurrentTime(0)
  
  // 根据播放模式处理
  if (musicStore.playMode === 'singleLoop') {
    // 单曲循环：重新播放当前歌曲
    setTimeout(() => {
      if (audioRef.value) {
        audioRef.value.currentTime = 0
        audioRef.value.play().catch(() => {})
      }
      musicStore.setCurrentTime(0)
      musicStore.setPlaying(true)
    }, 100)
  } else {
    // 顺序或随机模式：播放下一首
    const nextMusic = musicStore.playNext()
    if (nextMusic) {
      setTimeout(() => {
        musicStore.setPlaying(true)
      }, 100)
    }
  }
}

const handlePrevious = () => {
  if (musicStore.isPlaylistEmpty) {
    ElMessage.warning('播放列表为空')
    return
  }
  const prevMusic = musicStore.playPrevious()
  if (prevMusic) {
    musicStore.setPlaying(true)
  }
  // 顺序播放模式下现在支持循环，所以不需要提示"已经是第一首了"
}

const handleNext = () => {
  if (musicStore.isPlaylistEmpty) {
    ElMessage.warning('播放列表为空')
    return
  }
  const nextMusic = musicStore.playNext()
  if (nextMusic) {
    musicStore.setPlaying(true)
  }
  // 顺序播放模式下现在支持循环，所以不需要提示"已经是最后一首了"
}

const toggleMute = () => {
  if (!audioRef.value) return
  isMuted.value = !isMuted.value
  audioRef.value.muted = isMuted.value
}

const handleVolumeChange = (value: number) => {
  if (audioRef.value) {
    audioRef.value.volume = value / 100
    if (value > 0) isMuted.value = false
  }
}

watch(() => musicStore.isPlaying, async (playing) => {
  if (!audioRef.value) return
  try {
    if (playing) {
      if (!audioSrc.value) return
      if (audioRef.value.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA) {
        await audioRef.value.play()
      }
    } else {
      audioRef.value.pause()
    }
  } catch (e) {}
})

watch(() => musicStore.currentMusic, async (newMusic, oldMusic) => {
  if (!audioRef.value || !newMusic) return
  if (oldMusic && oldMusic.id === newMusic.id) return
  
  audioLoadError.value = false
  currentTime.value = 0
  musicStore.setCurrentTime(0)
  // 重置时长，等待新的音频加载
  musicStore.setDuration(0)
  
  try {
    audioRef.value.pause()
    audioRef.value.src = ''
    audioRef.value.load()
  } catch (e) {}
  
  await new Promise(resolve => setTimeout(resolve, 50))
  
  const newSrc = audioSrc.value
  if (!newSrc) return
  
  audioRef.value.src = newSrc
  audioRef.value.load()
  
  if (musicStore.isPlaying) {
    const canPlayHandler = () => {
      if (musicStore.isPlaying && audioRef.value) {
        audioRef.value.play().catch(() => {})
      }
    }
    audioRef.value.addEventListener('canplay', canPlayHandler, { once: true })
  }
})

onMounted(() => {
  if (audioRef.value) audioRef.value.volume = volume.value / 100
})
</script>

<style scoped lang="scss">
.audio-player-container {
  position: fixed;
  bottom: 30px;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  z-index: 2000;
  pointer-events: none;
  padding: 0 40px;
}

  // 左侧额外按钮样式已移除
  
  // 胶囊播放器主体
  .audio-player-capsule {
  pointer-events: auto;
  flex: 1;
  max-width: 1200px;
  min-width: 800px;
  height: 80px;
  background: rgba(20, 20, 20, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  transition: background var(--transition-base), border-color var(--transition-base), color var(--transition-base);
  
  // 浅色模式 - 白色背景，黑色文字和图标
  [data-theme="light"] & {
    background: rgba(255, 255, 255, 0.95) !important;
    border: 1px solid rgba(0, 0, 0, 0.1);
    color: #111827 !important;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
    
    // 歌曲信息 - 黑色文字
    .section-info {
      .meta-info {
        .title-row {
          .music-title {
            color: #111827 !important;
          }
          
          .music-artist {
            color: rgba(17, 24, 39, 0.7) !important;
          }
        }
        
        .action-row {
          .action-icon {
            color: rgba(17, 24, 39, 0.8) !important;
            
            &:hover {
              color: #111827 !important;
            }
            
            // 评论徽标
            .comment-badge {
              :deep(.el-badge__content) {
                color: rgba(17, 24, 39, 0.8) !important;
              }
            }
          }
        }
      }
    }
    
    // 控制按钮 - 黑色图标
    .section-control {
      .control-buttons {
        .ctrl-icon {
          color: rgba(17, 24, 39, 0.8) !important;
          
          &:hover {
            color: #111827 !important;
          }
          
          &.mode.active {
            color: #00c3ff !important;
          }
        }
      }
      
      .progress-bar-row {
        .time-label {
          color: rgba(17, 24, 39, 0.7) !important;
        }
        
        .slider-wrapper {
          .custom-slider {
            :deep(.el-slider__runway) {
              background-color: rgba(0, 0, 0, 0.1) !important;
            }
            
            :deep(.el-slider__bar) {
              background-color: #00c3ff !important;
            }
            
            :deep(.el-slider__button) {
              background-color: #00c3ff !important;
              border: 2px solid #00c3ff !important;
            }
          }
        }
      }
    }
    
    // 右侧功能区 - 黑色图标
    .section-features {
      .feat-icon {
        color: rgba(17, 24, 39, 0.8) !important;
        
        &:hover {
          color: #111827 !important;
        }
      }
      
      .lyrics-btn-icon {
        color: rgba(0, 195, 255, 0.9) !important;
        border: 1px solid rgba(0, 195, 255, 0.3) !important;
        
        &:hover {
          color: rgba(0, 195, 255, 1) !important;
          border-color: rgba(0, 195, 255, 0.5) !important;
        }
      }
    }
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    background: rgba(15, 23, 42, 0.95);
    border: 1px solid rgba(255, 255, 255, 0.15);
    color: var(--text-light);
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  }
  
  // 分区通用
  .section-info, .section-control, .section-features {
    height: 100%;
    display: flex;
    align-items: center;
  }

  // 1. 左侧：歌曲信息
  .section-info {
    width: 30%;
    gap: 16px;
    
    .cover-box {
      width: 56px;
      height: 56px;
      border-radius: 8px;
      position: relative;
      overflow: hidden;
      flex-shrink: 0;
      cursor: pointer;
      transition: transform 0.2s;
      
      &:hover {
        transform: scale(1.05);
        
        .cover-mask {
          opacity: 1;
        }
      }
      
      .music-cover {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      .cover-mask {
        position: absolute;
        inset: 0;
        background: rgba(0,0,0,0.3);
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.2s;
        
        .el-icon {
          font-size: 20px;
          color: #fff;
        }
      }
    }
    
    .meta-info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 6px;
      
      .title-row {
        display: flex;
        align-items: baseline;
        gap: 8px;
        white-space: nowrap;
        overflow: hidden;
        
        .music-title {
          font-size: 15px;
          font-weight: 500;
          color: #fff;
        }
        
        .music-artist {
          font-size: 12px;
          color: rgba(255, 255, 255, 0.6);
        }
      }
      
      .action-row {
        display: flex;
        align-items: center;
        gap: 14px;
        
        .action-icon {
          font-size: 18px;
          color: rgba(255, 255, 255, 0.6);
          cursor: pointer;
          transition: color 0.2s;
          
          &:hover { color: #fff; }
          
          // 评论徽标修正
          .comment-badge {
            :deep(.el-badge__content) {
              background-color: transparent;
              color: rgba(255, 255, 255, 0.8);
              border: none;
              font-size: 10px;
              top: -4px;
              right: -8px;
              transform: scale(0.8);
            }
          }
        }
      }
    }
  }

  // 2. 中间：控制区
  .section-control {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
    padding: 0 40px;
    
    .control-buttons {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 28px;
      
      .ctrl-icon {
        font-size: 20px;
        color: rgba(255, 255, 255, 0.7);
        cursor: pointer;
        transition: color 0.2s;
        
        &:hover { color: #fff; }
        
        &.mode {
          font-size: 18px;
          transition: all 0.2s;
          
          // 根据播放模式显示不同颜色
          &.active {
            color: #00c3ff;
            
            &:hover {
              color: #00d4ff;
              transform: scale(1.1);
            }
          }
          
          // 顺序模式：默认颜色
          // 循环模式：激活状态（蓝色）
          // 随机模式：激活状态（蓝色）
          &:hover {
            color: #fff;
            transform: scale(1.1);
          }
        }
      }
      
      .play-btn-circle {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: #00c3ff; // 青蓝色播放按钮
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: transform 0.2s;
        box-shadow: 0 4px 12px rgba(0, 195, 255, 0.3);
        
        .play-icon {
          font-size: 20px;
          color: #fff;
          margin-left: 2px;
        }
        
        &:hover { transform: scale(1.1); }
        &:active { transform: scale(0.95); }
      }
      
      .volume-box {
        position: relative;
        display: flex;
        align-items: center;
        height: 100%;
        
        &:hover .volume-slider-popup {
          opacity: 1;
          visibility: visible;
          transform: translateX(-50%) translateY(0);
        }
        
        .volume-slider-popup {
          position: absolute;
          bottom: 45px; // 调整弹出位置
          left: 50%;
          transform: translateX(-50%) translateY(10px);
          width: 36px;
          height: 110px;
          background: rgba(60, 40, 40, 0.95); // 深褐色背景
          backdrop-filter: blur(10px);
          border-radius: 18px;
          display: flex;
          justify-content: center;
          padding: 12px 0;
          opacity: 0;
          visibility: hidden;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
          border: 1px solid rgba(255, 255, 255, 0.1);
          z-index: 2001;
          
          // 小三角箭头
          &::after {
            content: '';
            position: absolute;
            bottom: -6px;
            left: 50%;
            transform: translateX(-50%);
            border-width: 6px 6px 0;
            border-style: solid;
            border-color: rgba(60, 40, 40, 0.95) transparent transparent transparent;
          }
          
          // 浅色模式
          [data-theme="light"] & {
            background: rgba(255, 255, 255, 0.95) !important;
            border: 1px solid rgba(0, 0, 0, 0.1) !important;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15) !important;
            
            &::after {
              border-color: rgba(255, 255, 255, 0.95) transparent transparent transparent !important;
            }
          }

          :deep(.el-slider.is-vertical) {
            .el-slider__runway {
              width: 4px;
              background-color: rgba(255, 255, 255, 0.2);
              margin: 0 16px;
              border-radius: 2px;
            }
            
            .el-slider__bar {
              width: 4px;
              background: #00c3ff; // 青蓝色
              border-radius: 2px;
            }
            
            .el-slider__button-wrapper {
              left: -16px;
              width: 36px;
              height: 36px;
            }
            
            .el-slider__button {
              width: 12px;
              height: 12px;
              border: 2px solid #fff;
              background-color: #00c3ff;
              box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
              transition: transform 0.2s;
            }
            
            &:hover .el-slider__button {
              transform: scale(1.2);
            }
            
            // 浅色模式
            [data-theme="light"] & {
              .el-slider__runway {
                background-color: rgba(0, 0, 0, 0.1) !important;
              }
              
              .el-slider__bar {
                background: #00c3ff !important;
              }
              
              .el-slider__button {
                border: 2px solid #00c3ff !important;
                background-color: #00c3ff !important;
              }
            }
          }
        }
      }
    }
    
    .progress-bar-row {
      width: 100%;
      display: flex;
      align-items: center;
      gap: 12px;
      
      .time-label {
        font-size: 11px;
        color: rgba(255, 255, 255, 0.5);
        min-width: 36px;
        font-family: monospace;
      }
      
      .slider-wrapper {
        flex: 1;
        display: flex;
        align-items: center;
        
        .custom-slider {
          width: 100%;
          
          :deep(.el-slider__runway) {
            height: 3px;
            background-color: rgba(255, 255, 255, 0.15);
            margin: 0;
          }
          
          :deep(.el-slider__bar) {
            height: 3px;
            background-color: #fff; // 白色进度条
          }
          
          :deep(.el-slider__button-wrapper) {
            width: 36px;
            height: 36px;
            top: -16px;
          }
          
          :deep(.el-slider__button) {
            width: 10px;
            height: 10px;
            border: none;
            background-color: #fff;
            box-shadow: 0 2px 4px rgba(0,0,0,0.3);
            transition: transform 0.2s;
          }
          
          &:hover :deep(.el-slider__button) {
            transform: scale(1.2);
          }
        }
      }
    }
  }

  // 3. 右侧：功能区
  .section-features {
    width: 20%;
    justify-content: flex-end;
    gap: 20px;
    
    .tag-hq {
      font-size: 10px;
      color: #00c3ff;
      border: 1px solid #00c3ff;
      padding: 1px 3px;
      border-radius: 3px;
      cursor: pointer;
      font-weight: 600;
      
      &:hover {
        background: rgba(0, 195, 255, 0.1);
      }
    }
    
    .feat-icon {
      font-size: 20px;
      color: rgba(255, 255, 255, 0.7);
      cursor: pointer;
      &:hover { color: #fff; }
    }
    
    .lyrics-btn-icon {
      font-size: 16px;
      color: rgba(255, 255, 255, 0.7);
      border: 1px solid rgba(255, 255, 255, 0.7);
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 500;
      
      &:hover {
        color: #fff;
        border-color: #fff;
      }
    }
  }
}

// 响应式
@media (max-width: 1200px) {
  .audio-player-capsule {
    min-width: unset;
    
    .section-info { width: 25%; }
    .section-features { width: 25%; }
  }
}
</style>
