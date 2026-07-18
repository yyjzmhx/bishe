<template>
  <header class="app-header">
    <div class="header-content">
      <div class="logo">
        <h1 class="logo-text text-gradient">MusicAI</h1>
      </div>
      
      <div class="header-actions">
        <el-button
          :icon="themeIcon"
          circle
          @click="toggleTheme"
        />
        <el-badge :value="3" class="notification-badge">
          <el-button :icon="Bell" circle />
        </el-badge>
        <el-dropdown @command="handleCommand">
          <el-avatar :src="userStore.userInfo?.avatar" :size="36">
            <el-icon><User /></el-icon>
          </el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人信息
              </el-dropdown-item>
              <el-dropdown-item v-if="userStore.isAdmin" command="admin">
                <el-icon><Setting /></el-icon>
                管理后台
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Moon, Sunny, User, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 从 localStorage 读取主题，如果没有则默认为 'light'
const savedTheme = localStorage.getItem('theme') || 'light'
const isDark = ref(savedTheme === 'dark')

// 确保初始状态与 localStorage 同步
if (document.documentElement.getAttribute('data-theme') !== savedTheme) {
  document.documentElement.setAttribute('data-theme', savedTheme)
  document.documentElement.style.colorScheme = savedTheme
}

const themeIcon = computed(() => isDark.value ? Sunny : Moon)

const toggleTheme = () => {
  isDark.value = !isDark.value
  const theme = isDark.value ? 'dark' : 'light'
  
  // 更新主题属性
  document.documentElement.setAttribute('data-theme', theme)
  document.documentElement.style.colorScheme = theme
  localStorage.setItem('theme', theme)
  
  console.log('🎨 主题切换为:', theme)
}

const handleCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'admin') {
    router.push('/admin/dashboard')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  }
}
</script>

<style scoped lang="scss">
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--header-height);
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  z-index: 1000;
  transition: background var(--transition-base), box-shadow var(--transition-base);
  
  // 暗色模式
  [data-theme="dark"] & {
    background: var(--bg-dark-secondary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }
  
  .header-content {
    max-width: 1920px;
    margin: 0 auto;
    height: 100%;
    display: flex;
    align-items: center;
    padding: 0 24px;
    gap: 24px;
  }
  
  .logo {
    .logo-text {
      font-size: 24px;
      font-weight: bold;
      font-family: 'Roboto', sans-serif;
    }
  }
  
  .header-actions {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .notification-badge {
      :deep(.el-badge__content) {
        background: var(--danger-color);
      }
    }
  }
}
</style>

