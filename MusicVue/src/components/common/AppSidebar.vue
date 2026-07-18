<template>
  <aside class="app-sidebar" :class="{ collapsed: isCollapsed }">
    <div class="sidebar-content">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :default-openeds="defaultOpeneds"
        router
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/">
          <el-icon><House /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <el-menu-item index="/upload">
          <el-icon><Upload /></el-icon>
          <template #title>音频分析推荐</template>
        </el-menu-item>

        <el-menu-item index="/history">
          <el-icon><Clock /></el-icon>
          <template #title>分析历史</template>
        </el-menu-item>

        <el-menu-item index="/favorites">
          <el-icon><Star /></el-icon>
          <template #title>我的收藏</template>
        </el-menu-item>

        <el-menu-item index="/community">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>音乐社区</template>
        </el-menu-item>

        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <template #title>个人信息</template>
        </el-menu-item>

        <el-menu-item index="/feedback">
          <el-icon><Bell /></el-icon>
          <template #title>消息通知</template>
        </el-menu-item>

        <el-sub-menu v-if="userStore.isAdmin" index="admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>管理后台</span>
          </template>

          <el-menu-item index="/admin/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>数据概览</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/music">
            <el-icon><VideoCamera /></el-icon>
            <span>曲库管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/uploads">
            <el-icon><Upload /></el-icon>
            <span>上传管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/feedback">
            <el-icon><Bell /></el-icon>
            <span>反馈管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/community">
            <el-icon><ChatDotRound /></el-icon>
            <span>社区管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/site-content">
            <el-icon><Picture /></el-icon>
            <span>站点内容</span>
          </el-menu-item>
          <el-menu-item index="/admin/notices">
            <el-icon><Bell /></el-icon>
            <span>通知公告</span>
          </el-menu-item>
          <el-menu-item index="/admin/ai-monitor">
            <el-icon><DataAnalysis /></el-icon>
            <span>AI 管理</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </div>

    <div class="sidebar-footer">
      <el-button
        :icon="isCollapsed ? Expand : Fold"
        circle
        text
        @click="toggleCollapse"
      />
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  ChatDotRound,
  Clock,
  DataAnalysis,
  Expand,
  Fold,
  House,
  Odometer,
  Picture,
  Setting,
  Star,
  Upload,
  User,
  VideoCamera
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapsed = ref(false)
const defaultOpeneds = ref<string[]>([])
const activeMenu = computed(() => route.path)

watch(() => route.path, (newPath) => {
  if (newPath.startsWith('/admin/')) {
    defaultOpeneds.value = ['admin']
  }
}, { immediate: true })

const handleMenuSelect = (index: string) => {
  if (route.path !== index) {
    router.push(index).catch((error) => {
      if (error?.name !== 'NavigationDuplicated') {
        console.error('菜单跳转失败', error)
      }
    })
  }
}

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}
</script>

<style scoped lang="scss">
.app-sidebar {
  position: fixed;
  left: 0;
  top: var(--header-height);
  bottom: var(--footer-height);
  width: var(--sidebar-width);
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  transition: width var(--transition-base), background var(--transition-base), box-shadow var(--transition-base);
  z-index: 999;

  &.collapsed {
    width: 64px;
  }

  .sidebar-content {
    height: calc(100% - 60px);
    overflow-y: auto;

    .sidebar-menu {
      border: none;
      height: 100%;
      background: transparent !important;

      :deep(.el-menu-item) {
        height: 56px;
        line-height: 56px;
        margin: 4px 12px;
        border-radius: var(--radius-sm);
        color: var(--text-dark);
        transition: all var(--transition-base);

        &:hover {
          background: rgba(59, 130, 246, 0.1);
        }

        &.is-active {
          background: var(--primary-gradient);
          color: white;

          .el-icon {
            color: white;
          }
        }
      }

      :deep(.el-sub-menu) {
        margin: 4px 12px;

        .el-sub-menu__title {
          height: 56px;
          line-height: 56px;
          border-radius: var(--radius-sm);
          color: var(--text-dark);
          transition: all var(--transition-base);

          &:hover {
            background: rgba(59, 130, 246, 0.1);
          }
        }

        &.is-opened .el-sub-menu__title {
          background: rgba(59, 130, 246, 0.1);
          color: var(--primary-from);
          font-weight: 600;
        }

        .el-menu {
          background: transparent;

          .el-menu-item {
            margin: 4px 0;
            padding-left: 48px !important;
            height: 48px;
            line-height: 48px;
          }
        }
      }
    }
  }

  .sidebar-footer {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-top: 1px solid var(--border-color);
  }
}
</style>

<style lang="scss">
[data-theme="dark"] .app-sidebar {
  background: var(--bg-dark-secondary) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;

  .sidebar-menu {
    background: transparent !important;
  }

  .sidebar-menu .el-menu-item {
    color: var(--text-light) !important;
    background-color: transparent !important;

    &:hover {
      background: rgba(64, 158, 255, 0.15) !important;
    }

    &.is-active {
      background: var(--primary-gradient) !important;
      color: white !important;

      .el-icon {
        color: white !important;
      }
    }
  }

  .sidebar-menu .el-sub-menu {
    .el-sub-menu__title {
      color: var(--text-light) !important;
      background-color: transparent !important;

      &:hover {
        background: rgba(64, 158, 255, 0.15) !important;
      }
    }

    &.is-opened .el-sub-menu__title {
      background: rgba(64, 158, 255, 0.15) !important;
    }

    .el-menu {
      background: transparent !important;

      .el-menu-item {
        color: var(--text-light) !important;
        background-color: transparent !important;

        &:hover {
          background: rgba(64, 158, 255, 0.12) !important;
        }

        &.is-active {
          background: var(--primary-gradient) !important;
          color: white !important;

          .el-icon {
            color: white !important;
          }
        }
      }
    }
  }
}
</style>
