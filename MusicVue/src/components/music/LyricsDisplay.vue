<template>
  <div class="lyrics-display">
    <!-- 歌曲信息 -->
    <div class="song-header">
      <div class="title-row">
        <h1 class="song-title">{{ music?.title || '未知歌曲' }}</h1>
        <el-tag v-if="showVIP" type="success" size="small" class="vip-tag">VIP</el-tag>
      </div>
      <p class="artist-name">{{ music?.artist || '未知艺术家' }}</p>
      <div v-if="music?.album" class="album-name">专辑：{{ music.album }}</div>
    </div>

    <!-- 歌词区域 -->
    <div class="lyrics-container" ref="lyricsContainerRef">
      <div class="lyrics-content">
        <!-- 没有歌词 -->
        <div v-if="!lyricsLines || lyricsLines.length === 0" class="lyrics-placeholder">
          <p class="lyrics-placeholder-text">暂无歌词</p>
          <p class="lyrics-placeholder-sub">
            当前播放时间：{{ formatTime(currentTime) }} / {{ formatTime(duration || props.music?.duration) }}
          </p>
        </div>

        <!-- 有歌词 - 只显示当前歌词周围的歌词 -->
        <div v-else class="lyrics-list">
          <!-- 顶部渐变遮罩 -->
          <div class="fade-top"></div>
          
          <!-- 歌词行容器 -->
          <div class="lyrics-lines-wrapper">
            <div
              v-for="(item, index) in visibleLyrics"
              :key="item.originalIndex"
              class="lyrics-line"
              :class="{ 
                'active': item.originalIndex === currentLyricsIndex,
                'before-active': item.originalIndex === currentLyricsIndex - 1,
                'after-active': item.originalIndex === currentLyricsIndex + 1
              }"
            >
              <span class="lyrics-text">{{ item.line.text || '\u00a0' }}</span>
            </div>
          </div>
          
          <!-- 底部渐变遮罩 -->
          <div class="fade-bottom"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { formatDuration } from '@/utils/format'
import { parseLRC, getCurrentLyricsIndex, type LyricsLine } from '@/utils/lyrics'
import type { Music } from '@/types/music'

const props = defineProps<{
  music: Music | null
  currentTime?: number
  duration?: number
}>()

// 监听 music 变化，调试用
watch(() => props.music, (newMusic) => {
  if (newMusic) {
    console.log('🎵 LyricsDisplay - 音乐数据更新:', {
      id: newMusic.id,
      title: newMusic.title,
      artist: newMusic.artist,
      hasTitle: !!newMusic.title
    })
  } else {
    console.warn('⚠️ LyricsDisplay - 音乐数据为空')
  }
}, { immediate: true, deep: true })

const showVIP = false
const lyricsContainerRef = ref<HTMLElement>()
const lyricsLines = ref<LyricsLine[]>([])
const currentLyricsIndex = ref(-1)

// 配置：显示当前歌词前后的歌词数量
const LYRICS_BEFORE = 2  // 当前歌词前面显示2条
const LYRICS_AFTER = 4    // 当前歌词后面显示4条
const TOTAL_VISIBLE = LYRICS_BEFORE + 1 + LYRICS_AFTER  // 总共显示7条

// 计算可见的歌词（只显示当前歌词周围的歌词）
const visibleLyrics = computed(() => {
  if (lyricsLines.value.length === 0) return []
  
  // 如果没有当前歌词，显示前几条
  if (currentLyricsIndex.value < 0) {
    return lyricsLines.value.slice(0, TOTAL_VISIBLE).map((line, index) => ({
      line,
      originalIndex: index
    }))
  }
  
  // 计算显示范围
  const startIndex = Math.max(0, currentLyricsIndex.value - LYRICS_BEFORE)
  const endIndex = Math.min(lyricsLines.value.length, currentLyricsIndex.value + LYRICS_AFTER + 1)
  
  // 如果前面不够，从后面补
  if (startIndex === 0 && endIndex < TOTAL_VISIBLE) {
    const actualEnd = Math.min(lyricsLines.value.length, TOTAL_VISIBLE)
    return lyricsLines.value.slice(0, actualEnd).map((line, index) => ({
      line,
      originalIndex: index
    }))
  }
  
  // 如果后面不够，从前面补
  if (endIndex === lyricsLines.value.length && startIndex > lyricsLines.value.length - TOTAL_VISIBLE) {
    const actualStart = Math.max(0, lyricsLines.value.length - TOTAL_VISIBLE)
    return lyricsLines.value.slice(actualStart).map((line, index) => ({
      line,
      originalIndex: actualStart + index
    }))
  }
  
  return lyricsLines.value.slice(startIndex, endIndex).map((line, index) => ({
    line,
    originalIndex: startIndex + index
  }))
})

// 解析歌词
const parseLyrics = () => {
  if (props.music?.lyrics) {
    lyricsLines.value = parseLRC(props.music.lyrics)
  } else {
    lyricsLines.value = []
  }
  currentLyricsIndex.value = -1
}

// 监听歌词变化和音乐对象变化
watch(() => props.music?.lyrics, () => {
  parseLyrics()
}, { immediate: true })

watch(() => props.music, () => {
  parseLyrics()
}, { immediate: true })

// 监听播放时间，更新当前歌词索引
watch(() => props.currentTime, (time) => {
  if (!time || time < 0) {
    currentLyricsIndex.value = -1
    return
  }

  const index = getCurrentLyricsIndex(lyricsLines.value, time)
  if (index !== currentLyricsIndex.value) {
    currentLyricsIndex.value = index
    // 由于只显示可见歌词，不需要滚动
  }
})

// 滚动到当前歌词（现在不需要滚动，因为我们只显示可见的歌词）
const scrollToCurrentLyrics = () => {
  // 由于只显示当前歌词周围的歌词，不需要滚动
  // 歌词会自动更新到可见区域
}

const formatTime = (seconds?: number) => {
  if (seconds === undefined || seconds === null || !isFinite(seconds) || isNaN(seconds)) return '00:00'
  return formatDuration(seconds)
}
</script>

<style scoped lang="scss">
.lyrics-display {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 32px;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.4) 0%,
    rgba(0, 0, 0, 0.2) 50%,
    rgba(0, 0, 0, 0.4) 100%
  );
  border-radius: 16px;
  position: relative;
  overflow: hidden;

  // 背景装饰
  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(
      circle at center,
      rgba(64, 158, 255, 0.05) 0%,
      transparent 70%
    );
    animation: rotate 20s linear infinite;
    pointer-events: none;
  }

  @keyframes rotate {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }
}

.song-header {
  margin-bottom: 24px;
  position: relative;
  z-index: 2;

  .title-row {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 12px;

    .song-title {
      font-size: 36px;
      font-weight: 700;
      margin: 0;
      line-height: 1.2;
      transition: all var(--transition-base);
      // 默认颜色（fallback）
      color: #fff;
      
      // 渐变文字效果
      background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.8) 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      text-shadow: 0 2px 20px rgba(255, 255, 255, 0.3);
      
      // 浅色模式
      [data-theme="light"] & {
        color: #111827; // fallback颜色
        background: linear-gradient(135deg, #111827 0%, rgba(17, 24, 39, 0.8) 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        text-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
      }
      
      // 暗色模式
      [data-theme="dark"] & {
        color: #D1D5DB; // fallback颜色
        background: linear-gradient(135deg, #D1D5DB 0%, rgba(209, 213, 219, 0.8) 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        text-shadow: 0 2px 20px rgba(255, 255, 255, 0.2);
      }
      
      // 如果浏览器不支持 background-clip: text，使用普通颜色
      @supports not (-webkit-background-clip: text) {
        -webkit-background-clip: unset;
        -webkit-text-fill-color: unset;
        background-clip: unset;
        background: none;
      }
    }

    .vip-tag {
      background: linear-gradient(135deg, #00d4aa 0%, #00b894 100%);
      border: none;
      font-weight: 600;
      padding: 4px 12px;
      box-shadow: 0 4px 12px rgba(0, 212, 170, 0.3);
    }
  }

  .artist-name {
    font-size: 20px;
    color: rgba(255, 255, 255, 0.8);
    margin: 0 0 8px 0;
    font-weight: 500;
    transition: color var(--transition-base);
    
    // 浅色模式
    [data-theme="light"] & {
      color: rgba(17, 24, 39, 0.8);
    }
    
    // 暗色模式
    [data-theme="dark"] & {
      color: rgba(209, 213, 219, 0.8);
    }
  }

  .album-name {
    font-size: 15px;
    color: rgba(255, 255, 255, 0.6);
    font-weight: 400;
    transition: color var(--transition-base);
    
    // 浅色模式
    [data-theme="light"] & {
      color: rgba(17, 24, 39, 0.6);
    }
    
    // 暗色模式
    [data-theme="dark"] & {
      color: rgba(209, 213, 219, 0.6);
    }
  }
}

.lyrics-container {
  flex: 1;
  overflow: hidden;
  padding: 0;
  background: transparent;
  border-radius: 0;
  min-height: 400px;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.lyrics-content {
  min-height: 250px;
  text-align: center;

  .lyrics-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 250px;
    padding: 40px 20px;

    .lyrics-placeholder-text {
      font-size: 20px;
      color: rgba(255, 255, 255, 0.5);
      margin: 0 0 20px 0;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 12px;
      transition: color var(--transition-base);

      &::before {
        content: '♪';
        font-size: 32px;
        opacity: 0.5;
      }
      
      // 浅色模式
      [data-theme="light"] & {
        color: rgba(17, 24, 39, 0.5);
      }
      
      // 暗色模式
      [data-theme="dark"] & {
        color: rgba(209, 213, 219, 0.5);
      }
    }

    .lyrics-placeholder-sub {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.4);
      margin: 0;
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
      background: rgba(255, 255, 255, 0.05);
      padding: 8px 16px;
      border-radius: 6px;
      border: 1px solid rgba(255, 255, 255, 0.1);
      transition: all var(--transition-base);
      
      // 浅色模式
      [data-theme="light"] & {
        color: rgba(17, 24, 39, 0.4);
        background: rgba(0, 0, 0, 0.05);
        border: 1px solid rgba(0, 0, 0, 0.1);
      }
      
      // 暗色模式
      [data-theme="dark"] & {
        color: rgba(209, 213, 219, 0.4);
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.1);
      }
    }
  }

  .lyrics-list {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 40px 0;
    min-height: 500px;

    // 顶部和底部渐变遮罩
    .fade-top,
    .fade-bottom {
      position: absolute;
      left: 0;
      right: 0;
      height: 120px;
      pointer-events: none;
      z-index: 10;
    }

    .fade-top {
      top: 0;
      background: linear-gradient(
        180deg,
        rgba(0, 0, 0, 0.8) 0%,
        rgba(0, 0, 0, 0.4) 50%,
        transparent 100%
      );
    }

    .fade-bottom {
      bottom: 0;
      background: linear-gradient(
        0deg,
        rgba(0, 0, 0, 0.8) 0%,
        rgba(0, 0, 0, 0.4) 50%,
        transparent 100%
      );
    }

    .lyrics-lines-wrapper {
      display: flex;
      flex-direction: column;
      gap: 20px;
      align-items: center;
      padding: 0 32px;
      position: relative;
      z-index: 1;
      transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
    }

    .lyrics-line {
      width: 100%;
      max-width: 800px;
      font-size: 16px;
      line-height: 1.8;
      color: rgba(255, 255, 255, 0.3);
      transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1), color var(--transition-base);
      padding: 14px 24px;
      min-height: 50px;
      white-space: pre-wrap;
      word-break: break-word;
      position: relative;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      text-align: center;
      opacity: 0.4;
      transform: scale(0.92);
      filter: blur(1px);

      .lyrics-text {
        display: block;
        width: 100%;
      }
      
      // 浅色模式
      [data-theme="light"] & {
        color: rgba(17, 24, 39, 0.3);
      }
      
      // 暗色模式
      [data-theme="dark"] & {
        color: rgba(255, 255, 255, 0.3);
      }

      // 当前歌词的前一条
      &.before-active {
        opacity: 0.75;
        color: rgba(255, 255, 255, 0.6);
        font-size: 18px;
        transform: scale(0.96);
        filter: blur(0.5px);
        
        [data-theme="light"] & {
          color: rgba(17, 24, 39, 0.6);
        }
        
        [data-theme="dark"] & {
          color: rgba(209, 213, 219, 0.6);
        }
      }

      // 当前歌词的后一条
      &.after-active {
        opacity: 0.75;
        color: rgba(255, 255, 255, 0.6);
        font-size: 18px;
        transform: scale(0.96);
        filter: blur(0.5px);
        
        [data-theme="light"] & {
          color: rgba(17, 24, 39, 0.6);
        }
        
        [data-theme="dark"] & {
          color: rgba(209, 213, 219, 0.6);
        }
      }

      // 当前激活的歌词
      &.active {
        opacity: 1;
        color: #fff;
        font-size: 30px;
        font-weight: 700;
        transform: scale(1);
        filter: blur(0);
        
        [data-theme="light"] & {
          color: var(--text-dark);
        }
        
        [data-theme="dark"] & {
          color: var(--text-light);
        }
        text-shadow: 
          0 0 30px rgba(64, 158, 255, 0.9),
          0 0 60px rgba(64, 158, 255, 0.5),
          0 4px 12px rgba(0, 0, 0, 0.4);
        background: linear-gradient(
          135deg,
          rgba(64, 158, 255, 0.3),
          rgba(64, 158, 255, 0.15),
          rgba(64, 158, 255, 0.1)
        );
        border: 2px solid rgba(64, 158, 255, 0.7);
        box-shadow: 
          0 8px 32px rgba(64, 158, 255, 0.5),
          0 0 0 1px rgba(255, 255, 255, 0.15) inset,
          0 0 80px rgba(64, 158, 255, 0.3);
        padding: 20px 36px;
        min-height: 75px;
        z-index: 5;

        // 激活行的光晕效果
        &::before {
          content: '';
          position: absolute;
          left: -2px;
          top: -2px;
          right: -2px;
          bottom: -2px;
          background: linear-gradient(
            135deg,
            rgba(64, 158, 255, 0.3),
            rgba(64, 158, 255, 0.1),
            rgba(64, 158, 255, 0.3)
          );
          border-radius: 12px;
          z-index: -1;
          animation: glow 2s ease-in-out infinite alternate;
        }

        // 激活行的装饰点
        &::after {
          content: '♪';
          position: absolute;
          left: 16px;
          top: 50%;
          transform: translateY(-50%);
          font-size: 20px;
          color: rgba(64, 158, 255, 0.8);
          opacity: 0.6;
        }
      }

      // 渐入动画
      @keyframes fadeInUp {
        from {
          opacity: 0;
          transform: translateY(20px) scale(0.9);
        }
        to {
          opacity: 1;
          transform: translateY(0) scale(1);
        }
      }

      @keyframes glow {
        from {
          opacity: 0.5;
          filter: blur(10px);
        }
        to {
          opacity: 0.8;
          filter: blur(15px);
        }
      }

      animation: fadeInUp 0.4s ease-out;
    }
  }
}
</style>
