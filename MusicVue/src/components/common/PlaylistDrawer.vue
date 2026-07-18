<template>
  <el-drawer
    :model-value="props.modelValue"
    @update:model-value="handleDrawerUpdate"
    :size="420"
    direction="rtl"
    class="playlist-drawer"
    :with-header="false"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    :append-to-body="true"
    :modal="true"
    @close="handleDrawerClose"
  >
    <div class="playlist-content">
      <!-- 自定义头部 -->
      <div class="playlist-header-custom">
        <h3 class="drawer-title">播放列表</h3>
        <el-button 
          circle 
          text 
          class="close-btn" 
          @click.stop="handleClose"
          :icon="Close"
        />
      </div>
      
      <!-- 播放模式切换 -->
      <div class="playlist-header">
        <div class="play-mode-selector">
          <span class="mode-label">播放模式</span>
          <el-button-group class="mode-button-group">
            <el-button
              size="small"
              :type="musicStore.playMode === 'order' ? 'primary' : 'default'"
              @click="handleSetPlayMode('order')"
            >
              <el-icon><Sort /></el-icon>
              <span>顺序</span>
            </el-button>
            <el-button
              size="small"
              :type="musicStore.playMode === 'singleLoop' ? 'primary' : 'default'"
              @click="handleSetPlayMode('singleLoop')"
            >
              <el-icon><Refresh /></el-icon>
              <span>单曲循环</span>
            </el-button>
            <el-button
              size="small"
              :type="musicStore.playMode === 'random' ? 'primary' : 'default'"
              @click="handleSetPlayMode('random')"
            >
              <el-icon><Operation /></el-icon>
              <span>随机</span>
            </el-button>
          </el-button-group>
        </div>
        <el-button
          type="danger"
          size="small"
          :disabled="musicStore.isPlaylistEmpty"
          :icon="Delete"
          @click="handleClearPlaylist"
        >
          清空
        </el-button>
      </div>

      <!-- 播放列表 -->
      <div v-if="musicStore.isPlaylistEmpty" class="empty-playlist">
        <el-empty description="播放列表为空" :image-size="120" />
      </div>
      <div v-else class="playlist-list">
        <div
          v-for="(music, index) in musicStore.playlist"
          :key="music.id"
          class="playlist-item"
          :class="{ active: index === musicStore.currentIndex }"
          @click="handlePlayMusic(index)"
        >
          <div class="item-cover">
            <img :src="music.coverUrl || defaultCover" :alt="music.title" />
            <div class="cover-mask" v-if="index === musicStore.currentIndex && musicStore.isPlaying">
              <el-icon class="playing-icon"><VideoPause /></el-icon>
            </div>
            <div class="cover-mask" v-else>
              <el-icon class="play-icon"><VideoPlay /></el-icon>
            </div>
          </div>
          <div class="item-info">
            <div class="item-title">{{ music.title }}</div>
            <div class="item-artist">{{ music.artist }}</div>
          </div>
          <div class="item-actions">
            <span class="item-index">{{ index + 1 }}</span>
            <el-button
              circle
              text
              size="small"
              class="delete-btn"
              @click.stop="handleDeleteMusic(index)"
              :icon="Delete"
              title="删除"
            />
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { VideoPlay, VideoPause, Delete, Close, Sort, Refresh, Operation } from '@element-plus/icons-vue'
import { useMusicStore } from '@/store/music'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const musicStore = useMusicStore()
const defaultCover = 'https://via.placeholder.com/64x64?text=Music'

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleDrawerUpdate = (val: boolean) => {
  emit('update:modelValue', val)
}

const handleDrawerClose = () => {
  emit('update:modelValue', false)
}

const handleSetPlayMode = (mode: 'order' | 'singleLoop' | 'random') => {
  musicStore.playMode = mode
  const modeNames = {
    order: '顺序播放',
    singleLoop: '单曲循环',
    random: '随机播放'
  }
  ElMessage.success(`已切换为${modeNames[mode]}`)
}

const handlePlayMusic = (index: number) => {
  // 确保索引有效
  if (index < 0 || index >= musicStore.playlist.length) {
    ElMessage.warning('无效的歌曲索引')
    return
  }
  
  const targetMusic = musicStore.playlist[index]
  if (!targetMusic) {
    ElMessage.warning('歌曲不存在')
    return
  }
  
  // 如果点击的是当前正在播放的歌曲
  if (index === musicStore.currentIndex && musicStore.currentMusic?.id === targetMusic.id) {
    // 如果正在播放，则暂停；如果已暂停，则继续播放
    musicStore.setPlaying(!musicStore.isPlaying)
    ElMessage.info(musicStore.isPlaying ? '已暂停' : '继续播放')
  } else {
    // 切换到新歌曲并开始播放
    musicStore.setCurrentIndex(index)
    musicStore.setPlaying(true)
    ElMessage.success(`正在播放: ${targetMusic.title}`)
  }
}

const handleDeleteMusic = async (index: number) => {
  try {
    await ElMessageBox.confirm('确定要从播放列表中移除这首歌曲吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    musicStore.removeFromPlaylist(index)
    ElMessage.success('已从播放列表移除')
  } catch {
    // 用户取消
  }
}

const handleClearPlaylist = async () => {
  try {
    await ElMessageBox.confirm('确定要清空播放列表吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    musicStore.clearPlaylist()
    ElMessage.success('播放列表已清空')
  } catch {
    // 用户取消
  }
}
</script>

<style lang="scss">
@import '@/assets/styles/mixins.scss';

// 注意：这里去掉了 scoped，因为 el-drawer append-to-body 后，scoped 样式可能无法穿透
// 我们使用 .playlist-drawer 作为顶层命名空间来隔离样式

.playlist-drawer {
  // 覆盖 Element Plus Drawer 的默认样式
  &.el-drawer__wrapper,
  &.el-overlay {
    z-index: 3000 !important;
    
    // 暗色模式下的遮罩层
    [data-theme="dark"] & {
      background-color: rgba(0, 0, 0, 0.7) !important;
    }
  }

  .el-drawer {
    background: rgba(255, 255, 255, 0.98) !important;
    backdrop-filter: blur(30px) saturate(180%);
    -webkit-backdrop-filter: blur(30px) saturate(180%);
    box-shadow: -4px 0 40px rgba(0, 0, 0, 0.15);
    border: none !important;
    outline: none !important;
    
    // 移除默认的 focus outline
    &:focus {
      outline: none !important;
    }
    
    .el-drawer__body {
      padding: 0;
      background: transparent !important;
      height: 100%;
    }
  }

  // 暗色模式样式强力覆盖
  [data-theme="dark"] & {
    .el-drawer {
      background: #0f172a !important; /* 使用纯色或极深色覆盖白色底色 */
      box-shadow: -4px 0 40px rgba(0, 0, 0, 0.8) !important;
      border-left: 1px solid rgba(255, 255, 255, 0.05) !important; // 给一个微弱的边框代替白边
    }
    
    // 确保内部所有可能透出白色的元素都透明
    .el-drawer__body,
    .el-drawer__header {
      background: transparent !important;
    }
  }
}

// 下面是内容区域的样式，可以保持原有逻辑，但要注意层级
.playlist-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(
    180deg, 
    rgba(255, 255, 255, 0.98) 0%, 
    rgba(249, 250, 251, 0.95) 50%,
    rgba(255, 255, 255, 0.98) 100%
  );
  position: relative;
  
  // 装饰性渐变背景
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 200px;
    background: linear-gradient(
      135deg,
      rgba(0, 195, 255, 0.05) 0%,
      rgba(0, 136, 255, 0.03) 50%,
      transparent 100%
    );
    pointer-events: none;
    z-index: 0;
  }
  
  > * {
    position: relative;
    z-index: 1;
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    background: linear-gradient(
      180deg,
      #0f172a 0%, /* 使用 hex 颜色确保不透明 */
      #141e30 50%,
      #0f172a 100%
    ) !important;
    
    &::before {
      background: linear-gradient(
        135deg,
        rgba(0, 195, 255, 0.08) 0%,
        rgba(0, 136, 255, 0.05) 50%,
        transparent 100%
      );
    }
  }
}


.playlist-header-custom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(249, 250, 251, 0.9) 100%
  );
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  position: relative;
  z-index: 1;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  
  .drawer-title {
    font-size: 22px;
    font-weight: 800;
    margin: 0;
    background: var(--primary-gradient);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    letter-spacing: -0.5px;
    position: relative;
    
    &::after {
      content: '';
      position: absolute;
      bottom: -4px;
      left: 0;
      width: 40px;
      height: 3px;
      background: var(--primary-gradient);
      border-radius: 2px;
      opacity: 0.6;
    }
  }
  
  .close-btn {
    width: 36px;
    height: 36px;
    padding: 0;
    color: var(--text-gray);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    pointer-events: auto;
    cursor: pointer;
    border-radius: 50%;
    
    &:hover {
      color: var(--primary-color);
      background: rgba(0, 195, 255, 0.1);
      transform: rotate(90deg) scale(1.1);
      box-shadow: 0 4px 12px rgba(0, 195, 255, 0.2);
    }
    
    &:active {
      transform: rotate(90deg) scale(0.95);
    }
    
    :deep(.el-icon) {
      font-size: 20px;
      pointer-events: none;
    }
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    border-bottom-color: rgba(255, 255, 255, 0.08) !important;
    background: linear-gradient(
      135deg,
      rgba(15, 23, 42, 1) 0%,
      rgba(20, 30, 48, 0.98) 100%
    ) !important;
    
    .drawer-title {
      background: var(--primary-gradient);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    
    .close-btn {
      color: rgba(255, 255, 255, 0.7);
      
      &:hover {
        color: #fff;
        background: rgba(0, 195, 255, 0.2);
      }
    }
  }
}

.playlist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 20px 28px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  position: relative;
  
  .el-button--danger {
    flex-shrink: 0;
  }

  .play-mode-selector {
    display: flex;
    align-items: center;
    gap: 14px;
    flex: 0 0 auto;
    min-width: 0;

    .mode-label {
      font-size: 13px;
      color: var(--text-gray);
      font-weight: 700;
      white-space: nowrap;
      flex-shrink: 0;
      letter-spacing: 0.3px;
    }
    
    .mode-button-group {
      display: flex;
      border-radius: 10px;
      overflow: hidden;
      box-shadow: 
        0 2px 8px rgba(0, 0, 0, 0.08),
        inset 0 1px 0 rgba(255, 255, 255, 0.5);
      flex-shrink: 0;
      background: rgba(255, 255, 255, 0.6);
      padding: 2px;
      gap: 2px;
      
      .el-button {
        border-radius: 8px;
        font-size: 12px;
        padding: 8px 18px;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        display: flex;
        align-items: center;
        gap: 6px;
        border: none;
        position: relative;
        font-weight: 600;
        
        &.el-button--primary {
          background: var(--primary-gradient);
          color: #fff;
          box-shadow: 
            0 2px 8px rgba(0, 195, 255, 0.4),
            0 0 16px rgba(0, 195, 255, 0.2);
          transform: scale(1.02);
          
          .el-icon {
            color: #fff;
          }
          
          &:hover {
            transform: scale(1.05);
            box-shadow: 
              0 4px 12px rgba(0, 195, 255, 0.5),
              0 0 20px rgba(0, 195, 255, 0.3);
          }
        }
        
        &:not(.el-button--primary) {
          background: transparent;
          color: var(--text-gray);
          
          &:hover {
            background: rgba(0, 195, 255, 0.08);
            color: var(--primary-color);
            transform: translateY(-1px);
            
            .el-icon {
              color: var(--primary-color);
            }
          }
        }
        
        .el-icon {
          font-size: 14px;
          transition: transform 0.3s;
        }
        
        &:hover .el-icon {
          transform: scale(1.1);
        }
        
        span {
          font-size: 12px;
          font-weight: inherit;
        }
      }
    }
  }
  
  .el-button--danger {
    border-radius: 8px;
    font-size: 12px;
    padding: 8px 18px;
    font-weight: 600;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 2px 6px rgba(239, 68, 68, 0.2);
    
    &:not(:disabled):hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
    }
    
    &:not(:disabled):active {
      transform: translateY(0);
    }
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    border-bottom-color: rgba(255, 255, 255, 0.08) !important;
    background: rgba(15, 23, 42, 0.95) !important;
    
    .play-mode-selector {
      .mode-label {
        color: rgba(255, 255, 255, 0.6);
      }

      .mode-button-group {
        background: rgba(0, 0, 0, 0.4) !important;
        box-shadow: 
          0 2px 8px rgba(0, 0, 0, 0.4),
          inset 0 1px 0 rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.05) !important;
        
        .el-button:not(.el-button--primary) {
          color: rgba(255, 255, 255, 0.7);
          background: transparent !important;
          
          &:hover {
            background: rgba(0, 195, 255, 0.15) !important;
            color: var(--primary-color);
          }
        }
        
        // 分隔线修复
        .el-button + .el-button {
          border-left: 1px solid rgba(255, 255, 255, 0.1);
        }
      }
    }
  }
}

.empty-playlist {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 200px;
    height: 200px;
    background: radial-gradient(
      circle,
      rgba(0, 195, 255, 0.08) 0%,
      transparent 70%
    );
    border-radius: 50%;
    pointer-events: none;
  }
  
  :deep(.el-empty) {
    position: relative;
    z-index: 1;
    
    .el-empty__image {
      opacity: 0.6;
    }
    
    .el-empty__description {
      color: var(--text-gray);
      font-size: 15px;
      font-weight: 500;
      margin-top: 16px;
    }
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    &::before {
      background: radial-gradient(
        circle,
        rgba(0, 195, 255, 0.12) 0%,
        transparent 70%
      );
    }
  }
}

.playlist-list {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 0;
  scroll-behavior: smooth;
  
  // 自定义滚动条
  &::-webkit-scrollbar {
    width: 10px;
  }
  
  &::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.03);
    border-radius: 10px;
    margin: 12px 8px;
    border: 1px solid rgba(0, 0, 0, 0.05);
  }
  
  &::-webkit-scrollbar-thumb {
    background: linear-gradient(
      180deg,
      rgba(0, 195, 255, 0.3) 0%,
      rgba(0, 136, 255, 0.4) 100%
    );
    border-radius: 10px;
    transition: all 0.3s;
    border: 2px solid transparent;
    background-clip: padding-box;
    
    &:hover {
      background: linear-gradient(
        180deg,
        rgba(0, 195, 255, 0.5) 0%,
        rgba(0, 136, 255, 0.6) 100%
      );
      background-clip: padding-box;
    }
  }
  
  // 暗色模式滚动条
  [data-theme="dark"] & {
    &::-webkit-scrollbar-track {
      background: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.1);
    }
    
    &::-webkit-scrollbar-thumb {
      background: linear-gradient(
        180deg,
        rgba(0, 195, 255, 0.4) 0%,
        rgba(0, 136, 255, 0.5) 100%
      );
      
      &:hover {
        background: linear-gradient(
          180deg,
          rgba(0, 195, 255, 0.6) 0%,
          rgba(0, 136, 255, 0.7) 100%
        );
      }
    }
  }
}

.playlist-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  margin: 0 20px 12px 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border: 1.5px solid rgba(255, 255, 255, 0.8);
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.06),
    0 0 0 0 rgba(0, 195, 255, 0);
  pointer-events: auto;
  user-select: none;
  position: relative;
  overflow: hidden;

  // 装饰性渐变背景
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(
      135deg,
      rgba(0, 195, 255, 0) 0%,
      rgba(0, 195, 255, 0.03) 100%
    );
    opacity: 0;
    transition: opacity 0.4s;
    pointer-events: none;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.98);
    transform: translateX(-8px) translateY(-2px);
    box-shadow: 
      0 8px 24px rgba(0, 0, 0, 0.12),
      0 0 0 4px rgba(0, 195, 255, 0.1);
    border-color: rgba(0, 195, 255, 0.3);

    &::before {
      opacity: 1;
    }

    .cover-mask {
      opacity: 1;
    }

    .delete-btn {
      opacity: 1;
    }
    
    .item-index {
      opacity: 0;
      width: 0;
      margin: 0;
    }
    
    .item-title {
      color: var(--primary-color);
      transform: translateX(2px);
    }
    
    .item-cover {
      transform: scale(1.08);
      box-shadow: 0 6px 20px rgba(0, 195, 255, 0.3);
    }
  }
  
  &:active {
    transform: translateX(-8px) translateY(-1px) scale(0.98);
  }

  &.active {
    background: linear-gradient(
      135deg, 
      rgba(0, 195, 255, 0.15) 0%, 
      rgba(0, 136, 255, 0.12) 100%
    );
    border-color: rgba(0, 195, 255, 0.5);
    box-shadow: 
      0 8px 28px rgba(0, 195, 255, 0.3),
      0 0 0 4px rgba(0, 195, 255, 0.15),
      inset 0 1px 0 rgba(255, 255, 255, 0.2);
    transform: translateX(-8px);

    &::before {
      opacity: 1;
      background: linear-gradient(
        135deg,
        rgba(0, 195, 255, 0.1) 0%,
        rgba(0, 136, 255, 0.08) 100%
      );
    }

    .item-title {
      color: var(--primary-color);
      font-weight: 700;
    }
    
    .item-cover {
      box-shadow: 
        0 8px 24px rgba(0, 195, 255, 0.5),
        0 0 0 2px rgba(0, 195, 255, 0.3);
      transform: scale(1.1);
    }
    
    .item-index {
      color: var(--primary-color);
      font-weight: 700;
    }
  }
  
  // 暗色模式
  [data-theme="dark"] & {
    background: rgba(20, 30, 48, 0.95) !important;
    border-color: rgba(255, 255, 255, 0.08) !important;
    box-shadow: 
      0 4px 16px rgba(0, 0, 0, 0.4),
      0 0 0 0 rgba(0, 195, 255, 0);
    
    &:hover {
      background: rgba(25, 35, 53, 1) !important;
      border-color: rgba(0, 195, 255, 0.4) !important;
    }
    
    &.active {
      background: linear-gradient(
        135deg,
        rgba(0, 195, 255, 0.2) 0%,
        rgba(0, 136, 255, 0.15) 100%
      ) !important;
      border-color: rgba(0, 195, 255, 0.6) !important;
    }

    .item-info {
      .item-title {
        color: #fff !important;
      }
      .item-artist {
        color: rgba(255, 255, 255, 0.6) !important;
      }
    }

    .item-actions {
      .item-index {
        color: rgba(255, 255, 255, 0.4) !important;
      }
      .delete-btn {
        color: rgba(255, 255, 255, 0.6) !important;
        
        &:hover {
          color: var(--danger-color) !important;
          background: rgba(239, 68, 68, 0.2) !important;
        }
      }
    }
  }

  .item-cover {
    width: 56px;
    height: 56px;
    border-radius: 14px;
    overflow: hidden;
    position: relative;
    flex-shrink: 0;
    box-shadow: 
      0 4px 12px rgba(0, 0, 0, 0.15),
      0 0 0 2px rgba(255, 255, 255, 0.5);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    pointer-events: none;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.4s;
    }
    
    &:hover img {
      transform: scale(1.1);
    }

    .cover-mask {
      position: absolute;
      inset: 0;
      background: linear-gradient(
        135deg,
        rgba(0, 195, 255, 0.7) 0%,
        rgba(0, 136, 255, 0.6) 100%
      );
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
      color: #fff;
      backdrop-filter: blur(4px);
      -webkit-backdrop-filter: blur(4px);
      pointer-events: none;

      .el-icon {
        font-size: 24px;
        transform: scale(0.8);
        transition: transform 0.3s;
      }
    }
    
    &:hover .cover-mask .el-icon {
      transform: scale(1);
    }
    
    // 暗色模式
    [data-theme="dark"] & {
      box-shadow: 
        0 4px 12px rgba(0, 0, 0, 0.5),
        0 0 0 2px rgba(255, 255, 255, 0.1) !important;
    }
  }

  .item-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
    pointer-events: none;

    .item-title {
      font-size: 15px;
      font-weight: 500;
      color: var(--text-dark);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      transition: all 0.3s;
      line-height: 1.4;
    }

    .item-artist {
      font-size: 12px;
      color: var(--text-gray);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      line-height: 1.4;
    }
  }

  .item-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    min-width: 60px;
    justify-content: flex-end;

    .item-index {
      font-size: 13px;
      color: var(--text-light-gray);
      font-weight: 500;
      min-width: 24px;
      text-align: center;
      transition: all 0.3s;
      margin-right: 4px;
    }

    .delete-btn {
      width: 32px;
      height: 32px;
      padding: 0;
      opacity: 0.6;
      visibility: visible;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      color: var(--text-gray);
      flex-shrink: 0;
      
      &:hover {
        opacity: 1;
        color: var(--danger-color);
        background: rgba(239, 68, 68, 0.15);
        transform: scale(1.1);
      }
      
      &:active {
        transform: scale(0.95);
      }
      
      :deep(.el-icon) {
        font-size: 18px;
      }
    }
  }
}
</style>

