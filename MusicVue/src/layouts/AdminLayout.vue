<template>
  <div class="admin-layout">
    <AppHeader />
    <div class="layout-content">
      <AdminSidebar />
      <main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/common/AppHeader.vue'
import AdminSidebar from '@/components/common/AdminSidebar.vue'
import AppFooter from '@/components/common/AppFooter.vue'
</script>

<style scoped lang="scss">
.admin-layout {
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
  padding: 32px; // 增加内边距
  overflow-y: auto;
  // 添加渐变背景
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  margin-left: 256px;
  transition: margin-left var(--transition-base);
  min-height: calc(100vh - var(--header-height) - var(--footer-height));
  
  // 暗色模式背景
  [data-theme="dark"] & {
    background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  }
  
  // 当侧边栏折叠时调整
  :deep(.admin-sidebar.collapsed ~ &) {
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

