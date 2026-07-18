<template>
  <div class="music-card" @click="handleClick">
    <div class="card-cover">
      <img :src="music.coverUrl || defaultCover" :alt="music.title" />
      <div class="cover-overlay">
        <el-button
          :icon="isCurrentPlaying ? VideoPause : VideoPlay"
          circle
          type="primary"
          size="large"
          @click.stop="handlePlay"
        />
        <div
          v-if="previewMode && isPreviewing && musicStore.currentMusic?.id === music.id"
          class="preview-badge"
        >
          试听中...
        </div>
      </div>
      <div v-if="music.duration" class="duration-badge">
        {{ formatDuration(music.duration) }}
      </div>
      <div v-if="showSimilarity && similarity !== undefined" class="similarity-badge">
        相似度 {{ formatSimilarity(similarity) }}
      </div>
    </div>

    <div class="card-info">
      <h3 class="card-title" :title="music.title">{{ music.title }}</h3>
      <p class="card-artist" :title="music.artist">{{ music.artist }}</p>

      <div class="card-stats">
        <span class="stat-item">
          <el-icon><VideoCamera /></el-icon>
          {{ music.playCount || 0 }}
        </span>
        <span class="stat-item">
          <el-icon><Star /></el-icon>
          {{ music.likeCount || 0 }}
        </span>
      </div>

      <div class="card-actions">
        <el-button
          :icon="isFavorite ? StarFilled : Star"
          text
          :type="isFavorite ? 'warning' : ''"
          @click.stop="handleFavorite"
        />

        <el-dropdown
          trigger="click"
          @command="handlePlaylistCommand"
          @click.stop
        >
          <el-button
            :icon="isInPlaylist ? Check : Plus"
            text
            :type="isInPlaylist ? 'success' : ''"
            @click.stop
          />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="add" :disabled="isInPlaylist">
                <el-icon><Plus /></el-icon>
                <span>加入播放列表</span>
              </el-dropdown-item>
              <el-dropdown-item command="addNext" :disabled="isInPlaylist">
                <el-icon><ArrowDown /></el-icon>
                <span>下一首播放</span>
              </el-dropdown-item>
              <el-dropdown-item command="playNow">
                <el-icon><VideoPlay /></el-icon>
                <span>立即播放</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button :icon="ChatDotRound" text @click.stop="handleComment" />
        <el-button :icon="Share" text @click.stop="handleShare" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowDown,
  ChatDotRound,
  Check,
  Plus,
  Share,
  Star,
  StarFilled,
  VideoCamera,
  VideoPause,
  VideoPlay
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { checkFavoriteStatus, toggleFavorite } from '@/api/feedback'
import { useMusicStore } from '@/store/music'
import { formatDuration, formatSimilarity } from '@/utils/format'
import type { Music } from '@/types/music'

const props = defineProps<{
  music: Music
  showSimilarity?: boolean
  similarity?: number
  previewMode?: boolean
  initialFavorite?: boolean
}>()

const emit = defineEmits<{
  (e: 'favorite-change', payload: { musicId: number; isFavorite: boolean }): void
}>()

const router = useRouter()
const musicStore = useMusicStore()

const defaultCover = 'https://via.placeholder.com/300x300?text=Music'
const isFavorite = ref(false)
const isPreviewing = ref(false)
const previewTimer = ref<number | null>(null)
const previousPlayState = ref<{
  music: Music | null
  isPlaying: boolean
  currentTime: number
} | null>(null)

const isCurrentPlaying = computed(() => {
  return musicStore.isPlaying && musicStore.currentMusic?.id === props.music.id
})

const isInPlaylist = computed(() => {
  return musicStore.playlist.some(item => item.id === props.music.id)
})

const loadFavoriteStatus = async () => {
  if (!props.music.id) return

  if (typeof props.initialFavorite === 'boolean') {
    isFavorite.value = props.initialFavorite
    return
  }

  try {
    const result = await checkFavoriteStatus(props.music.id)
    isFavorite.value = result.isFavorite
  } catch (error) {
    console.error('加载收藏状态失败', error)
    isFavorite.value = false
  }
}

const handleClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (target.closest('.el-button') || target.closest('.el-dropdown') || target.closest('.el-dropdown-menu')) {
    return
  }
  router.push(`/music/${props.music.id}`)
}

const handlePlay = () => {
  if (props.previewMode) {
    handlePreviewPlay()
    return
  }

  if (musicStore.isPlaylistEmpty) {
    musicStore.addToPlaylist(props.music)
  } else if (!isInPlaylist.value) {
    musicStore.addToPlaylist(props.music)
  }

  musicStore.setCurrentMusicAndIndex(props.music)
  musicStore.setPlaying(!isCurrentPlaying.value || !musicStore.isPlaying)
}

const handlePreviewPlay = () => {
  if (isPreviewing.value && musicStore.currentMusic?.id === props.music.id) {
    stopPreview()
    return
  }

  if (musicStore.currentMusic && musicStore.isPlaying) {
    previousPlayState.value = {
      music: musicStore.currentMusic,
      isPlaying: true,
      currentTime: musicStore.currentTime
    }
    musicStore.setPlaying(false)
  } else {
    previousPlayState.value = null
  }

  musicStore.setCurrentMusic(props.music)
  musicStore.setPlaying(true)
  isPreviewing.value = true

  if (previewTimer.value) {
    clearTimeout(previewTimer.value)
  }

  previewTimer.value = window.setTimeout(() => {
    stopPreview()
  }, 15000)
}

const stopPreview = () => {
  isPreviewing.value = false

  if (previewTimer.value) {
    clearTimeout(previewTimer.value)
    previewTimer.value = null
  }

  musicStore.setPlaying(false)

  if (!previousPlayState.value) {
    musicStore.setCurrentMusic(null)
    return
  }

  const { music, isPlaying, currentTime } = previousPlayState.value
  previousPlayState.value = null

  if (!music) return

  if (!musicStore.playlist.some(item => item.id === music.id)) {
    musicStore.addToPlaylist(music)
  }

  musicStore.setCurrentMusicAndIndex(music)
  if (isPlaying) {
    setTimeout(() => {
      musicStore.setPlaying(true)
      musicStore.setCurrentTime(currentTime)
    }, 100)
  }
}

const handleFavorite = async () => {
  if (!props.music.id) return

  try {
    const result = await toggleFavorite(props.music.id)
    isFavorite.value = result.isFavorite
    emit('favorite-change', { musicId: props.music.id, isFavorite: result.isFavorite })
    ElMessage.success(result.isFavorite ? '已加入收藏夹' : '已移出收藏夹')
  } catch (error) {
    console.error('收藏操作失败', error)
  }
}

const handleComment = () => {
  router.push(`/music/${props.music.id}`)
}

const handleShare = async () => {
  const url = `${window.location.origin}/music/${props.music.id}`
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('歌曲链接已复制')
  } catch (error) {
    console.error('复制分享链接失败', error)
    ElMessage.info(url)
  }
}

const handlePlaylistCommand = (command: string) => {
  switch (command) {
    case 'add':
      handleAddToPlaylist()
      break
    case 'addNext':
      if (musicStore.isPlaylistEmpty) {
        handlePlay()
      } else {
        handleAddToPlaylistNext()
      }
      break
    case 'playNow':
      handlePlay()
      break
  }
}

const handleAddToPlaylist = () => {
  if (isInPlaylist.value) {
    ElMessage.warning('这首歌曲已在播放列表中')
    return
  }

  musicStore.addToPlaylist(props.music)
  ElMessage.success(`已将《${props.music.title}》加入播放列表`)
}

const handleAddToPlaylistNext = () => {
  if (isInPlaylist.value) {
    ElMessage.warning('这首歌曲已在播放列表中')
    return
  }

  const success = musicStore.addToPlaylistNext(props.music)
  if (success) {
    ElMessage.success(`已将《${props.music.title}》加入下一首`)
  }
}

watch(() => props.initialFavorite, (value) => {
  if (typeof value === 'boolean') {
    isFavorite.value = value
  }
}, { immediate: true })

watch(() => props.music.id, () => {
  loadFavoriteStatus()
}, { immediate: true })

watch(() => musicStore.currentTime, (newTime) => {
  if (props.previewMode && isPreviewing.value && musicStore.currentMusic?.id === props.music.id && newTime >= 15) {
    stopPreview()
    ElMessage.info('试听结束（15秒）')
  }
})

watch(() => musicStore.currentMusic, (newMusic) => {
  if (props.previewMode && isPreviewing.value && (!newMusic || newMusic.id !== props.music.id)) {
    stopPreview()
  }
})

onUnmounted(() => {
  if (previewTimer.value) {
    clearTimeout(previewTimer.value)
    previewTimer.value = null
  }

  if (isPreviewing.value && musicStore.currentMusic?.id === props.music.id) {
    stopPreview()
  }
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.music-card {
  @include card;
  cursor: pointer;
  overflow: hidden;
  transition: all var(--transition-base);

  &:hover {
    transform: translateY(-4px);
  }

  .card-cover {
    position: relative;
    width: 100%;
    aspect-ratio: 1;
    border-radius: var(--radius-sm);
    overflow: hidden;
    margin-bottom: 16px;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform var(--transition-base);
    }

    .cover-overlay {
      position: absolute;
      inset: 0;
      @include flex-center;
      background: rgba(0, 0, 0, 0.4);
      opacity: 0;
      transition: opacity var(--transition-base);
    }

    .duration-badge,
    .similarity-badge,
    .preview-badge {
      position: absolute;
      color: white;
      font-size: 12px;
      border-radius: 999px;
      padding: 4px 10px;
      z-index: 2;
    }

    .duration-badge {
      bottom: 8px;
      right: 8px;
      border-radius: 6px;
      background: rgba(0, 0, 0, 0.75);
    }

    .similarity-badge {
      top: 8px;
      left: 8px;
      background: var(--primary-gradient);
      font-weight: 600;
    }

    .preview-badge {
      top: 8px;
      right: 8px;
      background: rgba(0, 195, 255, 0.9);
      font-weight: 600;
    }

    &:hover {
      img {
        transform: scale(1.08);
      }

      .cover-overlay {
        opacity: 1;
      }
    }
  }

  .card-info {
    .card-title {
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 8px;
      @include text-ellipsis;
      color: var(--text-dark);
    }

    .card-artist {
      color: var(--text-gray);
      font-size: 14px;
      margin-bottom: 12px;
      @include text-ellipsis;
    }

    .card-stats {
      display: flex;
      gap: 16px;
      margin-bottom: 12px;
      font-size: 12px;
      color: var(--text-gray);

      .stat-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }

    .card-actions {
      display: flex;
      gap: 8px;
      padding-top: 12px;
      border-top: 1px solid var(--border-color);
    }
  }
}
</style>
