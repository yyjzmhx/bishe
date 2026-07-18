<template>
  <div class="music-detail-page">
    <!-- 返回按钮 -->
    <div class="page-header">
      <el-button :icon="ArrowLeft" text @click="handleBack">
        返回
      </el-button>
    </div>

    <!-- 主要内容区域 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="music" class="detail-content">
      <!-- 左侧：黑胶唱片播放器 -->
      <div class="left-section">
        <VinylPlayer 
          :music="music" 
          :is-playing="isPlaying"
          @play="handlePlay"
          @pause="handlePause"
        />
      </div>

      <!-- 右侧：歌曲信息与歌词 -->
      <div class="right-section">
      <LyricsDisplay 
        :music="music"
        :current-time="currentTime"
        :duration="duration"
      />
      </div>
    </div>

    <div v-else class="error-container">
      <el-empty description="音乐不存在">
        <el-button type="primary" @click="handleBack">返回</el-button>
      </el-empty>
    </div>

    <!-- 底部控制栏 -->
    <div class="bottom-control-bar" :class="{ 'has-music': music }">
      <div class="control-left">
        <div class="song-info" v-if="music">
          <span class="song-title">{{ music.title || '未知歌曲' }}</span>
          <span v-if="music.artist" class="song-artist"> - {{ music.artist }}</span>
        </div>
        <div class="song-info" v-else>
          <span class="song-title">加载中...</span>
        </div>
        <div class="action-buttons" v-if="music">
          <el-button
            :icon="isLiked ? StarFilled : Star"
            text
            :type="isLiked ? 'danger' : ''"
            @click="handleLike"
          >
            {{ music.likeCount || 0 }}
          </el-button>
          <el-button
            :icon="Plus"
            text
            @click="handleAddToPlaylist"
          >
            添加到播放列表
          </el-button>
          <el-button
            text
            :type="isFavorite ? 'warning' : ''"
            @click="handleFavorite"
          >
            {{ isFavorite ? '已收藏' : '收藏' }}
          </el-button>
          <el-button
            :icon="ChatDotRound"
            text
            @click.stop="handleShowComment"
            style="pointer-events: auto !important; z-index: 1002 !important;"
          >
            {{ commentTotal }}
          </el-button>
        </div>
      </div>

      <div class="control-center">
        <div class="control-buttons">
          <el-button
            :icon="Refresh"
            text
            circle
            @click="togglePlayMode"
          />
          <el-button
            :icon="CaretLeft"
            text
            circle
            @click="handlePrevious"
          />
          <el-button
            type="primary"
            :icon="isPlaying ? VideoPause : VideoPlay"
            circle
            size="large"
            @click="togglePlay"
          />
          <el-button
            :icon="CaretRight"
            text
            circle
            @click="handleNext"
          />
          <el-button
            :icon="isMuted ? Mute : Microphone"
            text
            circle
            @click="toggleMute"
          />
        </div>
        <div class="progress-bar">
          <span class="time-label">{{ formatTime(currentTime) }}</span>
          <el-slider
            v-model="currentTime"
            :max="duration"
            :show-tooltip="false"
            @change="handleSeek"
            @input="handleSliderInput"
            style="flex: 1; margin: 0 12px;"
          />
          <span class="time-label">{{ formatTime(duration) }}</span>
        </div>
      </div>

      <div class="control-right">
        <el-tag size="small" type="success">HQ</el-tag>
        <el-button :icon="Headset" text circle />
        <el-button text circle class="lyrics-btn">词</el-button>
        <el-button 
          :icon="Menu" 
          text 
          circle 
          @click.stop="showPlaylist = true"
          style="pointer-events: auto !important; z-index: 1002 !important;"
        />
      </div>
    </div>

    <!-- 评论区域抽屉 -->
    <el-drawer
      v-model="showCommentSection"
      :size="400"
      direction="rtl"
      :append-to-body="true"
      :modal="true"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      :with-header="false"
      class="comment-drawer"
    >
      <CommentSection
        :music-id="musicId"
        @comment-count-change="commentTotal = $event"
      />
    </el-drawer>

    <!-- 播放列表抽屉 -->
    <PlaylistDrawer v-model="showPlaylist" />

    <!-- 隐藏的音频元素 - 只有在有有效音频源时才渲染 -->
    <audio
      v-if="audioSrc && audioSrc.trim() !== ''"
      ref="audioRef"
      :src="audioSrc"
      @timeupdate="handleTimeUpdate"
      @loadedmetadata="handleLoadedMetadata"
      @ended="handleEnded"
      @error="handleAudioError"
      @canplay="handleCanPlay"
      preload="metadata"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft, Star, StarFilled, Plus, ChatDotRound,
  VideoPlay, VideoPause, CaretLeft, CaretRight, Refresh,
  Microphone, Mute, Headset, Menu
} from '@element-plus/icons-vue'
import { useMusicStore } from '@/store/music'
import { getMusicDetail } from '@/api/music'
import { checkFavoriteStatus, checkLikeStatus, toggleFavorite, toggleLike } from '@/api/feedback'
import { formatDuration } from '@/utils/format'
import { ElMessage } from 'element-plus'
import type { Music } from '@/types/music'
import VinylPlayer from '@/components/music/VinylPlayer.vue'
import LyricsDisplay from '@/components/music/LyricsDisplay.vue'
import CommentSection from '@/components/music/CommentSection.vue'
import PlaylistDrawer from '@/components/common/PlaylistDrawer.vue'

const route = useRoute()
const router = useRouter()
const musicStore = useMusicStore()

const musicId = computed(() => Number(route.params.id))
const music = ref<Music | null>(null)
const loading = ref(false)
const isLiked = ref(false)
const isFavorite = ref(false)
const commentTotal = ref(0)
const showCommentSection = ref(false)
const showPlaylist = ref(false)

// 调试：监听播放列表显示状态
watch(() => showPlaylist.value, (val) => {
  console.log('🎵 播放列表显示状态变化:', val)
})

const currentTime = ref(0)
const duration = ref(0)
const isMuted = ref(false)
const audioRef = ref<HTMLAudioElement>()
const isDragging = ref(false)

const isPlaying = computed(() => musicStore.isPlaying && musicStore.currentMusic?.id === musicId.value)

// 计算音频源URL
const audioSrc = computed(() => {
  // 如果音乐数据还未加载，返回空字符串（不渲染audio元素）
  if (!music.value) {
    return ''
  }
  
  if (!music.value.fileUrl) {
    console.warn('⚠️ 音乐没有fileUrl', {
      musicId: music.value.id,
      title: music.value.title,
      music: music.value
    })
    return ''
  }
  
  const fileUrl = music.value.fileUrl.trim()
  if (!fileUrl) {
    console.warn('⚠️ fileUrl为空字符串')
    return ''
  }
  
  console.debug('🎵 计算音频源URL - 原始fileUrl:', fileUrl)
  
  // 如果已经是 /api/files/ 开头的路径，直接返回
  if (fileUrl.startsWith('/api/files/')) {
    console.debug('✅ 使用/api/files/路径:', fileUrl)
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
        console.debug('✅ 转换后的路径 (方式1):', result)
        return result
      }
      
      if (pathParts.length > 1 && pathParts[0] === 'music-storage') {
        const filePath = pathParts.slice(1).join('/')
        const result = `/api/files/${filePath}`
        console.debug('✅ 转换后的路径 (方式2):', result)
        return result
      }
      
      const match = fileUrl.match(/music-storage[\/\\](.+)$/)
      if (match) {
        const result = `/api/files/${match[1]}`
        console.debug('✅ 转换后的路径 (方式3):', result)
        return result
      }
      
      console.warn('⚠️ 无法解析MinIO URL，返回原始URL:', fileUrl)
      return fileUrl
    } catch (e) {
      console.error('❌ URL解析失败:', e, '原始URL:', fileUrl)
      const match = fileUrl.match(/music-storage[\/\\](.+)$/)
      if (match) {
        const result = `/api/files/${match[1]}`
        console.debug('✅ 使用正则匹配转换:', result)
        return result
      }
      return fileUrl
    }
  }
  
  // 如果fileUrl是相对路径（如 library/music_2.mp3），直接添加 /api/files/ 前缀
  if (!fileUrl.startsWith('http://') && !fileUrl.startsWith('https://') && !fileUrl.startsWith('/')) {
    const result = `/api/files/${fileUrl}`
    console.debug('✅ 相对路径转换:', result)
    return result
  }
  
  console.debug('⚠️ 返回原始fileUrl:', fileUrl)
  return fileUrl
})

// 加载音乐详情
const loadMusicDetail = async () => {
  if (!musicId.value) {
    console.warn('⚠️ musicId为空，无法加载音乐详情')
    return
  }

  console.log('📥 开始加载音乐详情，ID:', musicId.value)
  loading.value = true
  try {
    const result = await getMusicDetail(musicId.value)
    console.log('✅ 音乐详情加载成功:', result)
    
    if (!result) {
      console.error('❌ 返回的音乐数据为空')
      ElMessage.error('音乐数据为空')
      return
    }
    
    if (!result.fileUrl) {
      console.warn('⚠️ 音乐数据中没有fileUrl:', result)
      ElMessage.warning('该音乐没有音频文件')
    }
    
    music.value = result

    // 检查点赞状态
    await Promise.all([checkLikeStatusFunc(), checkFavoriteStatusFunc()])

    // 如果当前播放的是这首歌，同步状态
    if (musicStore.currentMusic?.id === musicId.value) {
      const storeDuration = musicStore.duration
      duration.value = (storeDuration && isFinite(storeDuration)) ? storeDuration : (music.value.duration || 0)
    } else {
      duration.value = music.value.duration || 0
    }
    
    console.log('✅ 音乐详情加载完成，fileUrl:', music.value.fileUrl)
  } catch (error: any) {
    console.error('❌ 加载音乐详情失败', error)
    ElMessage.error('加载音乐详情失败: ' + (error.message || '未知错误'))
    music.value = null
  } finally {
    loading.value = false
  }
}

// 检查点赞状态
const checkLikeStatusFunc = async () => {
  if (!musicId.value) return
  try {
    const result = await checkLikeStatus(musicId.value)
    isLiked.value = result.isLiked
  } catch (error: any) {
    // 404错误可能是后端接口未实现，静默处理
    if (error?.response?.status === 404) {
      console.warn('点赞状态检查接口未实现，跳过')
      isLiked.value = false
    } else {
      console.error('检查点赞状态失败', error)
    }
  }
}

const checkFavoriteStatusFunc = async () => {
  if (!musicId.value) return
  try {
    const result = await checkFavoriteStatus(musicId.value)
    isFavorite.value = result.isFavorite
  } catch (error) {
    console.error('加载收藏状态失败', error)
    isFavorite.value = false
  }
}

// 处理返回
const handleBack = () => {
  router.push('/')
}

// 音频事件处理
const handleTimeUpdate = () => {
  if (audioRef.value && !isDragging.value) {
    currentTime.value = audioRef.value.currentTime
    musicStore.setCurrentTime(audioRef.value.currentTime)
  }
}

const handleLoadedMetadata = () => {
  if (audioRef.value) {
    const audioDuration = audioRef.value.duration
    if (audioDuration && isFinite(audioDuration)) {
      duration.value = audioDuration
      musicStore.setDuration(audioDuration)
    }
  }
}

const handleEnded = () => {
  musicStore.setPlaying(false)
  musicStore.setCurrentTime(0)
  currentTime.value = 0
  
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

const handleAudioError = (event: Event) => {
  const audio = event.target as HTMLAudioElement
  const audioSrcValue = audioSrc.value
  
  // 详细错误日志
  console.group('🔴 音频加载失败 - 详细信息')
  console.error('音频元素:', audio)
  console.error('错误对象:', audio.error)
  console.error('错误代码:', audio.error?.code)
  console.error('错误消息:', audio.error?.message)
  console.error('当前音频源:', audioSrcValue)
  console.error('音频元素src:', audio.src)
  console.error('音频元素readyState:', audio.readyState)
  console.error('音频元素networkState:', audio.networkState)
  console.error('原始fileUrl:', music.value?.fileUrl)
  console.error('音乐ID:', music.value?.id)
  console.error('音乐标题:', music.value?.title)
  console.groupEnd()
  
  // 检查网络请求
  if (audioSrcValue) {
    fetch(audioSrcValue, { method: 'HEAD' })
      .then(response => {
        console.log('文件请求状态:', response.status, response.statusText)
        if (!response.ok) {
          console.error('文件请求失败:', response.status, response.statusText)
        }
      })
      .catch(err => {
        console.error('文件请求异常:', err)
      })
  }
  
  if (audio.error) {
    let errorMsg = '音频加载失败'
    switch (audio.error.code) {
      case MediaError.MEDIA_ERR_ABORTED:
        // 用户中止加载，不显示错误
        console.log('用户中止加载，忽略错误')
        return
      case MediaError.MEDIA_ERR_NETWORK:
        errorMsg = '网络错误，无法加载音频文件'
        console.error('网络错误详情:', audio.error)
        break
      case MediaError.MEDIA_ERR_DECODE:
        errorMsg = '音频解码失败，文件可能已损坏'
        console.error('解码错误详情:', audio.error)
        break
      case MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED:
        errorMsg = '不支持的音频格式或文件不存在'
        console.error('源不支持详情:', audio.error)
        break
      default:
        errorMsg = `音频加载失败 (错误代码: ${audio.error.code})`
        console.error('未知错误:', audio.error)
    }
    ElMessage.error(errorMsg)
  } else {
    console.warn('音频错误对象为空，但触发了error事件')
    ElMessage.error('音频加载失败，请检查文件是否存在')
  }
  musicStore.setPlaying(false)
}

const handleCanPlay = () => {
  if (musicStore.isPlaying && audioRef.value) {
    audioRef.value.play().catch(() => {})
  }
}

// 播放控制
const handlePlay = async () => {
  if (music.value) {
    // 如果播放列表为空，先添加到播放列表
    if (musicStore.isPlaylistEmpty) {
      musicStore.addToPlaylist(music.value)
    }
    musicStore.setCurrentMusicAndIndex(music.value)
    musicStore.setPlaying(true)
  }
}

const handlePause = () => {
  musicStore.setPlaying(false)
}

const togglePlay = () => {
  if (music.value) {
    if (musicStore.currentMusic?.id === musicId.value) {
      if (musicStore.isPlaying) {
        musicStore.setPlaying(false)
      } else {
        musicStore.setPlaying(true)
      }
    } else {
      // 如果播放列表为空，先添加到播放列表
      if (musicStore.isPlaylistEmpty) {
        musicStore.addToPlaylist(music.value)
      }
      musicStore.setCurrentMusicAndIndex(music.value)
      musicStore.setPlaying(true)
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
    // watch 会自动处理路由跳转
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
    // watch 会自动处理路由跳转
  }
  // 顺序播放模式下现在支持循环，所以不需要提示"已经是最后一首了"
}

const togglePlayMode = () => {
  musicStore.togglePlayMode()
}

const toggleMute = () => {
  isMuted.value = !isMuted.value
  if (audioRef.value) {
    audioRef.value.muted = isMuted.value
  }
}

const handleSeek = (value: number) => {
  isDragging.value = false
  if (audioRef.value) {
    audioRef.value.currentTime = value
  }
  musicStore.setCurrentTime(value)
  currentTime.value = value
}

const handleSliderInput = (value: number) => {
  isDragging.value = true
  currentTime.value = value
  if (audioRef.value) {
    audioRef.value.currentTime = value
  }
  musicStore.setCurrentTime(value)
}

// 点赞处理
const handleLike = async () => {
  if (!musicId.value) return
  try {
    const result = await toggleLike(musicId.value)
    isLiked.value = result.isLiked
    if (music.value) {
      if (result.isLiked) {
        music.value.likeCount = (music.value.likeCount || 0) + 1
      } else {
        music.value.likeCount = Math.max((music.value.likeCount || 0) - 1, 0)
      }
    }
    ElMessage.success(result.isLiked ? '已点赞' : '已取消点赞')
  } catch (error: any) {
    console.error('点赞失败', error)
    ElMessage.error('点赞失败')
  }
}

// 添加到播放列表
const handleFavorite = async () => {
  if (!musicId.value) return
  try {
    const result = await toggleFavorite(musicId.value)
    isFavorite.value = result.isFavorite
    ElMessage.success(result.isFavorite ? '已加入收藏夹' : '已移出收藏夹')
  } catch (error) {
    console.error('收藏操作失败', error)
    ElMessage.error('收藏操作失败')
  }
}

const handleAddToPlaylist = () => {
  if (music.value) {
    musicStore.addToPlaylist(music.value)
    ElMessage.success('已添加到播放列表')
  }
}

// 显示评论抽屉
const handleShowComment = () => {
  console.log('📝 点击评论按钮，musicId:', musicId.value, 'showCommentSection:', showCommentSection.value)
  if (!musicId.value) {
    ElMessage.warning('歌曲ID不存在，无法加载评论')
    return
  }
  showCommentSection.value = true
  console.log('📝 评论抽屉已打开，showCommentSection:', showCommentSection.value)
}

const formatTime = (seconds: number) => formatDuration(seconds)

// 监听播放状态，同步到audio元素
watch(() => musicStore.isPlaying, async (playing) => {
  if (musicStore.currentMusic?.id === musicId.value && audioRef.value) {
    if (playing) {
      try {
        await audioRef.value.play()
      } catch (error) {
        console.error('播放失败', error)
        musicStore.setPlaying(false)
      }
    } else {
      audioRef.value.pause()
    }
  }
})

// 监听当前音乐变化
// 监听播放列表切换，自动跳转到新歌曲的详情页，并更新音频元素
watch(() => musicStore.currentMusic, (newMusic, oldMusic) => {
  // 如果当前在详情页，且播放列表切换到了不同的歌曲，自动跳转
  if (newMusic && newMusic.id && newMusic.id !== musicId.value) {
    // 避免重复跳转
    if (oldMusic?.id === newMusic.id) return
    
    console.log('🎵 播放列表切换歌曲，跳转到详情页:', newMusic.id)
    // 使用 replace 而不是 push，避免浏览器历史记录堆积
    router.replace(`/music/${newMusic.id}`)
    return // 跳转后不执行下面的逻辑
  }
  
  // 如果当前歌曲匹配，更新音频元素
  if (newMusic?.id === musicId.value && music.value) {
    const storeDuration = musicStore.duration
    duration.value = (storeDuration && isFinite(storeDuration)) ? storeDuration : (newMusic.duration || 0)
    
    // 等待音频元素加载新的src
    nextTick(() => {
      if (audioRef.value && audioSrc.value && audioSrc.value.trim() !== '') {
        audioRef.value.load()
      }
    })
  }
}, { immediate: true })

// 监听 music.value 变化，确保数据加载完成后再设置音频源
watch(() => music.value, (newMusic) => {
  if (newMusic && newMusic.fileUrl) {
    console.log('✅ 音乐数据加载完成，fileUrl:', newMusic.fileUrl)
    // 等待DOM更新后再加载音频
    nextTick(() => {
      if (audioRef.value && audioSrc.value && audioSrc.value.trim() !== '') {
        console.log('🔄 重新加载音频元素，src:', audioSrc.value)
        audioRef.value.load()
      }
    })
  }
})

watch(() => musicStore.duration, (newDuration) => {
  if (musicStore.currentMusic?.id === musicId.value) {
    duration.value = (newDuration && isFinite(newDuration)) ? newDuration : 0
  }
})

// 注意：不需要watch musicStore.currentTime，因为audio元素是源，handleTimeUpdate会更新musicStore
// 如果需要从外部（如其他组件）同步时间，可以取消注释下面的代码
// watch(() => musicStore.currentTime, (newTime) => {
//   if (musicStore.currentMusic?.id === musicId.value && !isDragging.value) {
//     currentTime.value = (newTime && isFinite(newTime)) ? newTime : 0
//     if (audioRef.value && Math.abs(audioRef.value.currentTime - newTime) > 0.5) {
//       audioRef.value.currentTime = newTime
//     }
//   }
// })

// 监听路由参数变化，当切换歌曲时重新加载详情
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    console.log('🔄 路由参数变化，重新加载音乐详情:', newId)
    loadMusicDetail()
  }
}, { immediate: true })

onMounted(() => {
  loadMusicDetail()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.music-detail-page {
  min-height: calc(100vh - var(--header-height) - var(--footer-height));
  background: 
    radial-gradient(
      circle at 20% 30%,
      rgba(64, 158, 255, 0.08) 0%,
      transparent 50%
    ),
    radial-gradient(
      circle at 80% 70%,
      rgba(0, 212, 170, 0.06) 0%,
      transparent 50%
    ),
    linear-gradient(135deg, #2d2d2d 0%, #1a1a1a 100%);
  padding: 32px;
  color: white;
  position: relative;
  overflow-x: hidden;
  transition: background var(--transition-base);

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: 
      radial-gradient(
        circle at 50% 50%,
        rgba(64, 158, 255, 0.03) 0%,
        transparent 70%
      );
    pointer-events: none;
    z-index: 0;
  }

  > * {
    position: relative;
    z-index: 1;
  }
  
  // 浅色模式
  [data-theme="light"] & {
    background: 
      radial-gradient(
        circle at 20% 30%,
        rgba(64, 158, 255, 0.05) 0%,
        transparent 50%
      ),
      radial-gradient(
        circle at 80% 70%,
        rgba(0, 212, 170, 0.03) 0%,
        transparent 50%
      ),
      linear-gradient(135deg, #f5f7fa 0%, #e2e8f0 100%);
    color: var(--text-dark);
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    background: 
      radial-gradient(
        circle at 20% 30%,
        rgba(64, 158, 255, 0.1) 0%,
        transparent 50%
      ),
      radial-gradient(
        circle at 80% 70%,
        rgba(0, 212, 170, 0.08) 0%,
        transparent 50%
      ),
      linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
    color: var(--text-light);
  }
}

.page-header {
  margin-bottom: 24px;
}

.loading-container,
.error-container {
  padding: 80px 0;
  text-align: center;
}

.detail-content {
  display: grid;
  grid-template-columns: 480px 1fr;
  gap: 56px;
  margin-bottom: 130px;
  padding: 20px 0;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
    gap: 40px;
  }
}

.left-section {
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.right-section {
  min-height: 450px;
}

.bottom-control-bar {
  position: fixed !important;
  bottom: var(--footer-height) !important;
  left: 0 !important;
  right: 0 !important;
  height: 90px !important;
  background: linear-gradient(
    180deg,
    rgba(20, 20, 20, 0.98) 0%,
    rgba(26, 26, 26, 0.95) 100%
  );
  backdrop-filter: blur(20px) saturate(180%);
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  padding: 0 32px;
  display: flex !important;
  align-items: center;
  justify-content: space-between;
  z-index: 1001 !important;
  box-shadow: 
    0 -4px 30px rgba(0, 0, 0, 0.5),
    0 -2px 10px rgba(64, 158, 255, 0.1);
  transition: all var(--transition-base);
  visibility: visible !important;
  opacity: 1 !important;
  pointer-events: auto !important;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(64, 158, 255, 0.3) 50%,
      transparent 100%
    );
  }
  
  // 浅色模式 - 白色圆角面板样式
  [data-theme="light"] & {
    background: #ffffff !important;
    border-top: 1px solid rgba(0, 0, 0, 0.08);
    border-radius: 24px 24px 0 0;
    box-shadow: 
      0 -4px 20px rgba(0, 0, 0, 0.08),
      0 -2px 8px rgba(0, 0, 0, 0.04);
    color: #111827 !important;
    backdrop-filter: none;
    
    &::before {
      display: none; // 隐藏渐变装饰线
    }
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    background: linear-gradient(
      180deg,
      rgba(15, 23, 42, 0.98) 0%,
      rgba(30, 41, 59, 0.95) 100%
    );
    border-top: 1px solid rgba(255, 255, 255, 0.15);
    box-shadow: 
      0 -4px 30px rgba(0, 0, 0, 0.6),
      0 -2px 10px rgba(64, 158, 255, 0.15);
    color: var(--text-light);
  }

  .control-left {
    flex: 0 0 320px;
    display: flex !important;
    flex-direction: column;
    gap: 10px;
    visibility: visible;
    opacity: 1;

    .song-info {
      font-size: 15px;
      color: rgba(255, 255, 255, 0.95);
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 6px;
      transition: color var(--transition-base);

      .song-title {
        font-weight: 600;
        background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.8) 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        transition: background var(--transition-base);
        // fallback颜色
        color: #fff;
      }

      .song-artist {
        color: rgba(255, 255, 255, 0.65);
        font-weight: 400;
        transition: color var(--transition-base);
      }
    }
    
    // 浅色模式 - 强制黑色文字
    [data-theme="light"] & {
      .song-info {
        color: #111827 !important;
        
        .song-title {
          color: #111827 !important;
          background: linear-gradient(135deg, #111827 0%, rgba(17, 24, 39, 0.8) 100%) !important;
          -webkit-background-clip: text !important;
          -webkit-text-fill-color: transparent !important;
          background-clip: text !important;
        }
        
        .song-artist {
          color: rgba(17, 24, 39, 0.7) !important;
        }
      }
    }

      .action-buttons {
      display: flex;
      gap: 6px;

      :deep(.el-button) {
        transition: all 0.3s ease;
        color: rgba(255, 255, 255, 0.8);
        
        &:hover {
          transform: translateY(-2px);
          background: rgba(255, 255, 255, 0.1);
          color: #fff;
        }
        
        :deep(.el-icon) {
          color: inherit !important;
        }
        
        // 按钮文字颜色
        span {
          color: inherit;
        }
      }
    }
    
    // 浅色模式 - 黑色文字和图标
    [data-theme="light"] & {
      .action-buttons :deep(.el-button) {
        color: #111827 !important;
        
        &:hover {
          background: rgba(0, 0, 0, 0.05) !important;
          color: #111827 !important;
        }
        
        :deep(.el-icon) {
          color: #111827 !important;
        }
        
        &:hover :deep(.el-icon) {
          color: #111827 !important;
        }
        
        // 按钮文字颜色
        span {
          color: #111827 !important;
        }
      }
    }
    
    // 暗色模式 - 白色文字和图标
    [data-theme="dark"] & {
      .song-info {
        color: rgba(255, 255, 255, 0.95);
        
        .song-title {
          color: #fff; // fallback
          background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.8) 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
        }
        
        .song-artist {
          color: rgba(255, 255, 255, 0.65);
        }
      }
      
      .action-buttons :deep(.el-button) {
        color: rgba(255, 255, 255, 0.8) !important;
        
        &:hover {
          background: rgba(255, 255, 255, 0.1);
          color: #fff !important;
        }
        
        :deep(.el-icon) {
          color: rgba(255, 255, 255, 0.8) !important;
        }
        
        &:hover :deep(.el-icon) {
          color: #fff !important;
        }
      }
    }
  }

  .control-center {
    flex: 1;
    max-width: 650px;
    display: flex !important;
    flex-direction: column;
    align-items: center;
    gap: 14px;
    visibility: visible;
    opacity: 1;

    .control-buttons {
      display: flex !important;
      align-items: center;
      gap: 8px;
      visibility: visible;
      opacity: 1;

      :deep(.el-button) {
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        color: rgba(255, 255, 255, 0.8);
        
        &:hover {
          transform: scale(1.1);
          background: rgba(255, 255, 255, 0.1);
          color: #fff;
        }
        
        :deep(.el-icon) {
          color: inherit !important;
        }

        &.is-circle {
          width: 40px;
          height: 40px;
        }

        &.el-button--large {
          width: 56px;
          height: 56px;
          background: linear-gradient(135deg, rgba(64, 158, 255, 0.9) 0%, rgba(64, 158, 255, 0.7) 100%);
          border: 2px solid rgba(255, 255, 255, 0.2);
          box-shadow: 
            0 4px 20px rgba(64, 158, 255, 0.4),
            0 0 30px rgba(64, 158, 255, 0.2);
          color: #fff;

          &:hover {
            transform: scale(1.15);
            box-shadow: 
              0 6px 25px rgba(64, 158, 255, 0.6),
              0 0 40px rgba(64, 158, 255, 0.3);
          }
          
          :deep(.el-icon) {
            color: #fff !important;
          }
        }
      }
    }
    
    // 浅色模式 - 优化控制按钮样式（黑色图标和文字）
    [data-theme="light"] & {
      .control-buttons :deep(.el-button) {
        color: #111827 !important;
        
        &:hover {
          background: rgba(0, 0, 0, 0.05) !important;
          color: #111827 !important;
        }
        
        :deep(.el-icon) {
          color: #111827 !important;
        }
        
        &:hover :deep(.el-icon) {
          color: #111827 !important;
        }
        
        // 按钮文字
        span {
          color: #111827 !important;
        }
        
        &.el-button--large {
          background: linear-gradient(135deg, #00c3ff 0%, #0088ff 100%) !important;
          border: none !important;
          color: #fff !important;
          box-shadow: 
            0 4px 16px rgba(0, 195, 255, 0.4),
            0 0 24px rgba(0, 195, 255, 0.2);

          &:hover {
            box-shadow: 
              0 6px 20px rgba(0, 195, 255, 0.5),
              0 0 32px rgba(0, 195, 255, 0.3);
          }
          
          :deep(.el-icon) {
            color: #fff !important;
          }
          
          span {
            color: #fff !important;
          }
        }
      }
    }
    
    // 暗色模式 - 白色图标和文字
    [data-theme="dark"] & {
      .control-buttons :deep(.el-button) {
        color: rgba(255, 255, 255, 0.8) !important;
        
        &:hover {
          background: rgba(255, 255, 255, 0.1);
          color: #fff !important;
        }
        
        :deep(.el-icon) {
          color: rgba(255, 255, 255, 0.8) !important;
        }
        
        &:hover :deep(.el-icon) {
          color: #fff !important;
        }
      }
    }

    .progress-bar {
      width: 100%;
      display: flex !important;
      align-items: center;
      gap: 12px;
      visibility: visible;
      opacity: 1;

      .time-label {
        font-size: 13px;
        color: rgba(255, 255, 255, 0.7);
        min-width: 45px;
        font-weight: 500;
        font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
        transition: color var(--transition-base);
      }

      :deep(.el-slider) {
        .el-slider__runway {
          background-color: rgba(255, 255, 255, 0.15);
          height: 4px;
          transition: background-color var(--transition-base);
        }

        .el-slider__bar {
          background: linear-gradient(90deg, rgba(64, 158, 255, 0.8) 0%, rgba(64, 158, 255, 1) 100%);
          height: 4px;
        }

        .el-slider__button {
          width: 14px;
          height: 14px;
          border: 2px solid rgba(64, 158, 255, 1);
          background: #fff;
          box-shadow: 0 2px 8px rgba(64, 158, 255, 0.5);
          transition: all 0.3s ease;

          &:hover {
            transform: scale(1.3);
            box-shadow: 0 4px 12px rgba(64, 158, 255, 0.7);
          }
        }
      }
    }
    
    // 浅色模式 - 优化进度条样式
    [data-theme="light"] & {
      .progress-bar {
        .time-label {
          color: rgba(17, 24, 39, 0.7);
        }
        
        :deep(.el-slider) {
          .el-slider__runway {
            background-color: rgba(0, 0, 0, 0.1);
          }
          
          .el-slider__bar {
            background: linear-gradient(90deg, #00c3ff 0%, #0088ff 100%);
          }
          
          .el-slider__button {
            border: 2px solid #00c3ff;
            background: #ffffff;
            box-shadow: 0 2px 8px rgba(0, 195, 255, 0.4);

            &:hover {
              box-shadow: 0 4px 12px rgba(0, 195, 255, 0.6);
            }
          }
        }
      }
    }
    
    // 暗色模式
    [data-theme="dark"] & {
      .progress-bar {
        .time-label {
          color: rgba(255, 255, 255, 0.7);
        }
        
        :deep(.el-slider) {
          .el-slider__runway {
            background-color: rgba(255, 255, 255, 0.15);
          }
        }
      }
    }
  }

  .control-right {
    flex: 0 0 220px;
    display: flex !important;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;
    visibility: visible;
    opacity: 1;

    :deep(.el-tag) {
      background: linear-gradient(135deg, rgba(0, 212, 170, 0.9) 0%, rgba(0, 212, 170, 0.7) 100%);
      border: none;
      padding: 4px 12px;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(0, 212, 170, 0.3);
    }

    :deep(.el-button) {
      transition: all 0.3s ease;
      color: rgba(255, 255, 255, 0.8);
      
      &:hover {
        transform: translateY(-2px);
        background: rgba(255, 255, 255, 0.1);
        color: #fff;
      }
      
      :deep(.el-icon) {
        color: inherit !important;
      }
    }

    .lyrics-btn {
      font-size: 13px;
      padding: 0;
      min-width: 36px;
      font-weight: 600;
      background: rgba(64, 158, 255, 0.2);
      border: 1px solid rgba(64, 158, 255, 0.3);
      color: rgba(64, 158, 255, 0.9);
      
      &:hover {
        background: rgba(64, 158, 255, 0.3);
        border-color: rgba(64, 158, 255, 0.5);
        color: rgba(64, 158, 255, 1);
      }
    }
    
    // 浅色模式 - 黑色图标和文字
    [data-theme="light"] & {
      :deep(.el-button) {
        color: #111827 !important;
        
        &:hover {
          background: rgba(0, 0, 0, 0.05) !important;
          color: #111827 !important;
        }
        
        :deep(.el-icon) {
          color: #111827 !important;
        }
        
        &:hover :deep(.el-icon) {
          color: #111827 !important;
        }
        
        // 按钮文字
        span {
          color: #111827 !important;
        }
      }
      
      :deep(.el-tag) {
        background: linear-gradient(135deg, #00c3ff 0%, #0088ff 100%) !important;
        color: #fff !important;
        border: none !important;
      }
      
      .lyrics-btn {
        background: rgba(0, 195, 255, 0.15) !important;
        border: 1px solid rgba(0, 195, 255, 0.3) !important;
        color: rgba(0, 195, 255, 0.9) !important;
        
        &:hover {
          background: rgba(0, 195, 255, 0.25) !important;
          border-color: rgba(0, 195, 255, 0.5) !important;
          color: rgba(0, 195, 255, 1) !important;
        }
      }
    }
    
    // 暗色模式 - 白色图标和文字
    [data-theme="dark"] & {
      :deep(.el-button) {
        color: rgba(255, 255, 255, 0.8) !important;
        
        &:hover {
          background: rgba(255, 255, 255, 0.1);
          color: #fff !important;
        }
        
        :deep(.el-icon) {
          color: rgba(255, 255, 255, 0.8) !important;
        }
        
        &:hover :deep(.el-icon) {
          color: #fff !important;
        }
      }
    }
  }
}

// 评论抽屉样式
:deep(.comment-drawer) {
  .el-drawer {
    z-index: 3000 !important;
    background: rgba(255, 255, 255, 0.98) !important;
    backdrop-filter: blur(30px) saturate(180%);
    -webkit-backdrop-filter: blur(30px) saturate(180%);
    box-shadow: -4px 0 40px rgba(0, 0, 0, 0.15);
    border: 0 !important;
    border-left: 0 !important;
    border-right: 0 !important;
    border-top: 0 !important;
    border-bottom: 0 !important;
    border-width: 0 !important;
    border-style: none !important;
    border-color: transparent !important;
    outline: none !important;
    
    &::before,
    &::after {
      display: none !important;
      border: none !important;
      content: none !important;
    }
  }
  
  .el-drawer__header {
    display: none !important;
    border: 0 !important;
    border-bottom: 0 !important;
    border-width: 0 !important;
    border-style: none !important;
    border-color: transparent !important;
  }
  
  .el-drawer__body {
    padding: 0 !important;
    background: transparent !important;
    border: 0 !important;
    border-width: 0 !important;
    border-style: none !important;
    border-color: transparent !important;
  }
  
  .el-overlay {
    z-index: 2999 !important;
    background: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(4px);
  }
  
  .el-drawer__wrapper {
    border: 0 !important;
    border-width: 0 !important;
    border-style: none !important;
    border-color: transparent !important;
    outline: none !important;
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    .el-drawer {
      background: rgba(15, 23, 42, 0.98) !important;
      box-shadow: -4px 0 40px rgba(0, 0, 0, 0.6);
      border: 0 !important;
      border-left: 0 !important;
      border-right: 0 !important;
      border-top: 0 !important;
      border-bottom: 0 !important;
      border-width: 0 !important;
      border-style: none !important;
      border-color: transparent !important;
      outline: none !important;
      
      &::before,
      &::after {
        display: none !important;
        border: none !important;
        content: none !important;
      }
    }
    
    .el-drawer__body {
      background: transparent !important;
      border: 0 !important;
      border-width: 0 !important;
      border-style: none !important;
      border-color: transparent !important;
    }
    
    .el-drawer__wrapper {
      border: 0 !important;
      border-width: 0 !important;
      border-style: none !important;
      border-color: transparent !important;
      outline: none !important;
    }
    
    .el-drawer__header {
      border: 0 !important;
      border-bottom: 0 !important;
      border-width: 0 !important;
      border-style: none !important;
      border-color: transparent !important;
    }
    
    .el-drawer__close-btn {
      border: 0 !important;
      border-width: 0 !important;
      border-style: none !important;
      border-color: transparent !important;
    }
    
    .el-overlay {
      background: rgba(0, 0, 0, 0.7);
    }
  }
}

// 全局样式：确保评论抽屉在暗色模式下无白边 - 使用最高优先级
// 我们使用 CSS 变量覆盖，这比覆盖具体属性更有效
html[data-theme="dark"] {
  .comment-drawer.el-drawer {
    --el-drawer-bg-color: #0f172a !important;
    --el-drawer-padding-primary: 0 !important;
    
    background-color: #0f172a !important;
    border: none !important;
    box-shadow: -4px 0 40px rgba(0, 0, 0, 0.6) !important;
    outline: none !important;
    
    .el-drawer__body {
      background-color: #0f172a !important;
      padding: 0 !important;
    }
    
    // 强制移除可能的边框
    &,
    .el-drawer__body,
    .el-drawer__header {
      border: none !important;
      outline: none !important;
    }
  }
}
</style>

