<template>
  <aside class="admin-sidebar" :class="{ collapsed: isCollapsed }">
    <div class="sidebar-content">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        router
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>数据仪表盘</template>
        </el-menu-item>
        
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        
        <el-menu-item index="/admin/music">
          <el-icon><VideoCamera /></el-icon>
          <template #title>音乐库管理</template>
        </el-menu-item>
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
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Odometer, User, VideoCamera, Fold, Expand } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// 处理菜单选择
const handleMenuSelect = (index: string) => {
  router.push(index)
}

// 确保侧边栏初始状态是展开的
watch(() => route.path, () => {
  // 路由变化时确保侧边栏展开
  if (isCollapsed.value && route.path.startsWith('/admin')) {
    isCollapsed.value = false
  }
}, { immediate: true })
</script>

<style scoped lang="scss">
@import '@/assets/styles/mixins.scss';

.admin-sidebar {
  position: fixed;
  left: 0;
  top: var(--header-height);
  bottom: var(--footer-height);
  width: 256px;
  min-width: 256px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  transition: width var(--transition-base);
  z-index: 999;
  
  &.collapsed {
    width: 64px;
    min-width: 64px;
  }
  
  .sidebar-content {
    height: calc(100% - 60px);
    overflow-y: auto;
    
    .sidebar-menu {
      border: none;
      height: 100%;
      
      :deep(.el-menu-item) {
        height: 56px;
        line-height: 56px;
        margin: 4px 12px;
        border-radius: var(--radius-sm);
        
        &:hover {
          background: rgba(59, 130, 246, 0.1);
        }
        
        &.is-active {
          background: var(--primary-gradient);
          color: white;
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

