<template>
  <div class="main-layout">
    <AppHeader />
    <div class="layout-content">
      <AppSidebar />
      <main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
    <AppFooter />
    <AudioPlayer v-if="shouldShowAudioPlayer" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useMusicStore } from '@/store/music'
import { getCurrentUser } from '@/api/auth'
import AppHeader from '@/components/common/AppHeader.vue'
import AppSidebar from '@/components/common/AppSidebar.vue'
import AppFooter from '@/components/common/AppFooter.vue'
import AudioPlayer from '@/components/common/AudioPlayer.vue'

const route = useRoute()
const userStore = useUserStore()
const musicStore = useMusicStore()

// 在音乐详情页隐藏全局AudioPlayer
// 如果播放列表有歌曲，也应该显示播放条（即使没有currentMusic）
const shouldShowAudioPlayer = computed(() => {
  // 在音乐详情页隐藏全局AudioPlayer
  if (route.name === 'MusicDetail') {
    return false
  }
  
  // 如果有当前播放的音乐，显示播放条
  if (musicStore.currentMusic) {
    return true
  }
  
  // 如果播放列表有歌曲，也显示播放条（让用户可以选择播放）
  if (musicStore.playlist.length > 0) {
    return true
  }
  
  return false
})

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      const userInfo = await getCurrentUser()
      userStore.setUserInfo(userInfo as any)
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }
  
  // 如果已登录，加载播放列表和播放状态
  if (userStore.isLoggedIn) {
    musicStore.loadPlaylistFromServer()
  }
})
</script>

<style scoped lang="scss">
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-content {
  flex: 1;
  display: flex;
  margin-top: var(--header-height);
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  margin-left: var(--sidebar-width);
  transition: margin-left var(--transition-base);
  min-height: calc(100vh - var(--header-height) - var(--footer-height));
  
  // 暗色模式背景
  [data-theme="dark"] & {
    background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  }
  
  // 当侧边栏折叠时调整
  :deep(.app-sidebar.collapsed ~ &) {
    margin-left: 64px;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-base);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

